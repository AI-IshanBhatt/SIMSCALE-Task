package BO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class Utils {

    static final String DATE_UTC_REGEX = "([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.*?Z)";
    static final String TRACE_SERVICE_REGEX = "([a-zA-Z0-9]*?)";
    static final String SPAN_REGEX = "([a-zA-Z0-9]*?)->([a-zA-Z0-9]*)";

    public static Timestamp stringToDateUTC(String date_str) {
        DateFormat df1 = new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        DateFormat df2 = new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Date date;
        Timestamp ts = null;
        try {
            date = df1.parse(date_str);
            ts = new Timestamp(date.getTime());
        } catch (ParseException e) {
            try {
                date = df2.parse(date_str);
                ts = new Timestamp(date.getTime());
            } catch (ParseException ex) {
                System.out.println("IGNORING INVALID DATE FORMAT "+date_str);
            }
        }
        return ts;
    }

    public static String timeStampToStr(Timestamp timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(timestamp);
    }

    /**
     * Creates Map (traceId -> List of requests) for given list of requests
     * @param requests  List of request
     * @return          Grouped by traceId Map
     */
    public static Map<String, List<Request>> groupByTraceId(List<Request> requests) {

        return requests.stream().collect(groupingBy(Request::getTraceId));
    }

    /**
     * Merge a list of map into a single map concatinating the lists of the same key(traceId)
     * @param traceIdToRequestList  List of Map(traceId->List of request)
     * @return                      Consolidated map of traceId -> List of requests
     */
    public static Map<String, List<Request>> mergeTraceIdGroups(List<Map<String, List<Request>>> traceIdToRequestList) {
        return traceIdToRequestList.stream().parallel()
                .flatMap(m -> m.entrySet().stream())
                .collect(groupingBy(Map.Entry::getKey,
                        Collector.of(
                                ArrayList::new,  // Supplier
                                (list, item) -> list.addAll(item.getValue()),  // Accumulator
                                (left, right) -> {  // Combiner
                                    left.addAll(right);
                                    return left;
                                }
                        )));
    }

    /**
     * A recursive function to create a nested Root object from list of requests
     * @param request       root request(found by findRoot)
     * @param requestList   list of request corresponding to root request
     * @return              Root object(nested)
     */
    private static Root makeRoot(Request request, List<Request> requestList) {
        String start = timeStampToStr(request.getStartTime());
        String end = timeStampToStr(request.getEndTime());
        String service = request.getServiceName();
        String span = request.getNewSpan();
        ArrayList<Root> calls = new ArrayList<>();

        for (Request r: findChildren(request, requestList))
        {
            Root z = makeRoot(r, requestList);
            calls.add(z);
        }

        return new Root(service, start, end, span, calls);
    }

    /**
     * Converts request object to it's corresponding Root object
     * @param request   Request object
     * @return          Root object
     */
    private static Root requestToRoot(Request request) {
        String start = timeStampToStr(request.getStartTime());
        String end = timeStampToStr(request.getEndTime());
        String service = request.getServiceName();
        String span = request.getNewSpan();
        ArrayList<Root> calls = new ArrayList<>();
        return new Root(service, start, end, span, calls);
    }

    /**
     * An iterative function to create a nested Root object from list of requests
     * @param request       root request(found by findRoot)
     * @param requestList   list of request corresponding to root request
     * @return              Root object(nested)
     */
    public static Root makeRootIter(Request request, List<Request> requestList) {
        Queue<Request> requestStack = new LinkedList<>();
        requestStack.add(request);
        Map<Request, Root> requestRootMap = new HashMap<>();
        requestRootMap.put(request, requestToRoot(request));

        while (requestStack.size() != 0) {
            Request popped = requestStack.remove();
            for(Request r: findChildren(popped, requestList))
            {
                Root parent = requestRootMap.get(popped);
                Root current = requestToRoot(r);
                parent.getCalls().add(current);
                requestRootMap.putIfAbsent(r, current);
                requestStack.add(r);
            }
        }
        return requestRootMap.get(request);
    }

    /**
     * From a list of request find the starting point which is a request with oldSpan as null
     * @param requests  List of request with same traceId
     * @return          the root/starting of the request
     */
    public static Request findRoot(List<Request> requests) {
        return requests.stream().filter(request -> request.getPrevSpan().equals("null")).findAny().get();
    }


    /**
     * Find children from the requests based on their new span and sorted by startTime
     * @param root      The root of the request found by findRoot method
     * @param requests  List of requests with same traceId
     * @return          List of root's immediate children sorted by their startTime
     */
    public static List<Request> findChildren(Request root, List<Request> requests) {
        String newSpan = root.getNewSpan();
        return requests.stream()
                .filter(request -> request.getPrevSpan().equals(newSpan))
                .sorted(Comparator.comparing(Request::getStartTime))
                .collect(Collectors.toList());

    }

    /**
     * Converts list of requests with same traceId to nested Trace object
     * @param traceId   Trace-id string
     * @param requests  List of requests having trace-id same as traceId
     * @return          Trace object(N level nested) for traceId
     */
    public static Trace traceMapToTraceObj(String traceId, List<Request> requests) {
        if(requests.size() == 1)
        {
            return new Trace(traceId, new Root(requests.get(0).getServiceName(), timeStampToStr(requests.get(0).getStartTime()),
                    timeStampToStr(requests.get(0).getEndTime()), requests.get(0).getNewSpan(), new ArrayList<>()));
        }
        else {
            Request rootRequest = findRoot(requests);
            return new Trace(rootRequest.getTraceId(), makeRoot(rootRequest, requests));
        }
    }

    /**
     * Converts Trace object to json string which can directly be written into file
     * @param trace     Nested Trace object
     * @return          Json representation of Trace object
     * @throws JsonProcessingException
     */
    public static String traceToJsonString(Trace trace) throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(trace);
    }

}

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

    public static final String DATE_UTC_REGEX = "([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.*?Z)";
    public static final String TRACE_SERVICE_REGEX = "([a-zA-Z0-9]*?)";
    public static final String SPAN_REGEX = "([a-zA-Z0-9]*?)->([a-zA-Z0-9]*)";

    public static Timestamp stringToDateUTC(String date_str) {
        DateFormat df1 = new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        DateFormat df2 = new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Date date = null;
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

    public static Map<String, List<Request>> groupByTraceId(List<Request> requests) {

        Map<String, List<Request>> traceGroups = requests.stream().collect(groupingBy(Request::getTraceId));
        return traceGroups;
    }

    public static Map<String, List<Request>> mergeTraceIdGroups(List<Map<String, List<Request>>> traceIdToRequestList) {
        return traceIdToRequestList.stream().parallel()
                .flatMap(m -> m.entrySet().stream())
                .collect(groupingBy(Map.Entry::getKey,
                        Collector.of(
                                ArrayList::new,  // Supplier
                                (list, item) -> list.addAll(item.getValue()),  // Consumer
                                (left, right) -> {  // Combiner
                                    left.addAll(right);
                                    return left;
                                }
                        )));
    }

    public static Root makeRoot(Request request, List<Request> requestList) {
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


    public static Request findRoot(List<Request> requests) {
        return requests.stream().filter(request -> request.getPrevSpan().equals("null")).findAny().get();
    }

    public static List<Request> findChildren(Request root, List<Request> requests) {
        String newSpan = root.getNewSpan();
        return requests.stream()
                .filter(request -> request.getPrevSpan().equals(newSpan))
                .sorted(Comparator.comparing(Request::getStartTime))
                .collect(Collectors.toList());

    }

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

    public static String traceToJsonString(Trace trace) throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String traceJson = objectMapper.writeValueAsString(trace);
        return traceJson;
    }


}

package app;

import BO.Request;
import BO.Trace;
import BO.Utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Starter {
    public static void main(String[] args) throws IOException{

        String rawLogs;
        String outputTrace;
        try {
            rawLogs = args[0];
            outputTrace = args[1];
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("INPUT and OUTPUT FIles are mandetory parameters");
        }

        String fileName = rawLogs;
        int threshold = 100;
        int counter = 1;
        String lineContent;
        List<Request> requests = new ArrayList<>();
        List<List<Request>> masterList =  new ArrayList<>();
        try(BufferedReader br =
                    new BufferedReader(new FileReader(fileName))){
            while ((lineContent = br.readLine()) != null) {
                try {
                    if (counter <= threshold - 1) {
                        counter++;
                        requests.add(Request.parseLine(lineContent).get());
                    } else {
                        counter = 1;
                        requests.add(Request.parseLine(lineContent).get());  // Line for this iteration
                        masterList.add(new ArrayList<>(requests));
                        requests.clear();
//                        System.out.println("A CHUNK HAS BEEN ADDED"); // FIND THE MEMORY USAGE HERE
                    }
                } catch (NoSuchElementException e) {} // Ignoring the malformed lines
            }
            // For the remaining chunk
            masterList.add(new ArrayList<>(requests));
            requests.clear();
        } catch (IOException e) {
            throw new IOException("Raw logs file was not found", e);
        }


        System.out.println("LEN OF CHUNKS "+masterList.size());

        int noOfProcessors = Runtime.getRuntime().availableProcessors();

        // As the below computation is not IO heavy, keep the size of pool as noOfProcessors not more
        ExecutorService executorService = Executors.newFixedThreadPool(noOfProcessors);

        /*
        * Note to myself
        * We could have created list of Callables first with passing Supplier as () -> Utils.groupByTraceId(requestList)
        * And then using invokeAll(callables)
        * And then doing get on FutureObject like below
        * List<Map<String, List<Request>>> allTraceidGroups = futures.stream().map(mapFuture -> mapFuture.get()).collect(Collectors.toList());
        * BUT, if the future object throws exception, there was no way to catch it
        * So, we will take advantage of CompletableFuture which allows us to use our executor as well
        * */
        List<CompletableFuture<Map<String, List<Request>>>> futures = masterList.stream()
                .map(requestsList -> CompletableFuture.supplyAsync(() -> Utils.groupByTraceId(requestsList), executorService))
                .collect(Collectors.toList());

        // This is one Map per chunk, they need to be merged
        List<Map<String, List<Request>>> traceIdToRequestList = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        executorService.shutdown();  // Do not forget this call, otherwise app will never return

        Map<String, List<Request>> flattenedTraceIdToRequests = Utils.mergeTraceIdGroups(traceIdToRequestList);

        String longestTraceId = Collections.max(
                flattenedTraceIdToRequests.entrySet(),
                Comparator.comparingInt(e -> e.getValue().size())).getKey();

        int longestTraceLength = flattenedTraceIdToRequests.get(longestTraceId).size();
        Double averageTraceSize = flattenedTraceIdToRequests
                .entrySet()
                .stream()
                .collect(Collectors.averagingInt(value -> value.getValue().size()));

        System.out.println("TOTAL Trace Json size "+flattenedTraceIdToRequests.size());
        System.out.println("TraceId "+longestTraceId+ " is longest with size "+longestTraceLength);
        System.out.println("Average trace size "+ averageTraceSize);

        PrintWriter printWriter = new PrintWriter(new FileWriter(outputTrace));
        for(Map.Entry<String, List<Request>> entry: flattenedTraceIdToRequests.entrySet()) {
            Trace trace = Utils.traceMapToTraceObj(entry.getKey(), entry.getValue());
            String traceJson = Utils.traceToJsonString(trace);
            printWriter.println(traceJson);
        }
        printWriter.close();
        System.out.println("WRITING DONE");
    }
}

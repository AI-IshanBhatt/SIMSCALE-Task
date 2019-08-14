package BO;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static BO.Utils.*;


public class Request {

    private final Timestamp startTime;
    private final Timestamp endTime;
    private final String traceId;
    private final String serviceName;
    private final String prevSpan;
    private final String newSpan;


    // The one who creates this object, will give it as timestamp
    public Request(Timestamp startTime, Timestamp endTime, String traceId, String serviceName, String prevSpan, String newSpan) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.traceId = traceId;
        this.serviceName = serviceName;
        this.prevSpan = prevSpan;
        this.newSpan = newSpan;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPrevSpan() { return prevSpan; }

    public String getNewSpan() { return newSpan; }

    @Override
    public String toString() {
        return "Request{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", traceId='" + traceId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", oldSpan='" + prevSpan + '\'' +
                ", newSpan='" + newSpan + '\'' +
                '}';
    }

    public static Optional<Request> parseLine(String logLine) {
        String dateRegex = DATE_UTC_REGEX;
        String traceServiceRegex = TRACE_SERVICE_REGEX;
        String spanRegex = SPAN_REGEX;

        String logRegex = dateRegex+" "+dateRegex+" "+traceServiceRegex+" "+traceServiceRegex+" "+spanRegex;
        Pattern p = Pattern.compile(logRegex);
        Matcher m = p.matcher(logLine);

        Request r = null;

        if (m.find()) {
            Timestamp startTime = Utils.stringToDateUTC(m.group(1));
            Timestamp endTime = Utils.stringToDateUTC(m.group(2));
            String traceId = m.group(3);
            String serviceName = m.group(4);
            String prevSpan = m.group(5);
            String newSpan = m.group(6);
            r = new Request(startTime, endTime, traceId, serviceName, prevSpan, newSpan);
        }
        else {
            System.out.println("SOMETHING WRONG IN MATCHING OF LINE "+logLine+ " IGNORING");
        }

        return Optional.ofNullable(r);
    }

}

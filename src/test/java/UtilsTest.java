import BO.Request;
import BO.Trace;
import BO.Utils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    private static List<Request> requests1;
    private static List<Request> requests2;
    private static List<Request> requestsWithNull;

    @BeforeAll
    public static void setUpBeforeClass() {
        requests1 = new ArrayList<>();
        requests1.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t1", "s1", "span1", "span2"));
        requests1.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t1", "s2", "span1", "span2"));
        requests1.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t1", "s2", "span2", "span3"));
        requests1.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t2", "s3", "span2", "span3"));

        requests2 = new ArrayList<>();
        requests2.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t3", "s1", "span1", "span2"));
        requests2.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t3", "s2", "span1", "span2"));
        requests2.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t3", "s2", "span2", "span3"));
        requests2.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t2", "s3", "span2", "span3"));

        requestsWithNull = new ArrayList<>();
        requestsWithNull.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.571"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t3", "s1", "span1", "span2"));
        requestsWithNull.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.471"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t3", "s2", "span3", "span5"));
        requestsWithNull.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.371"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t3", "s2", "span3", "span4"));
        requestsWithNull.add(new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.971"), "t3", "s3", "null", "span3"));

    }

    @Test
    public void stringToDateUTC() {
        Timestamp correctFormat = Utils.stringToDateUTC("2013-10-23T10:12:35.271Z");
        Timestamp ts = Timestamp.valueOf("2013-10-23 10:12:35.271");
        assertEquals(ts, correctFormat);
        Timestamp randomTS = Utils.stringToDateUTC("SOME RANDOM STRING");
        assertNull(randomTS);
    }

    @Test
    public void timeStampToStr() {
        Timestamp ts = Timestamp.valueOf("2013-10-23 10:12:35.271");
        assertEquals("2013-10-23T10:12:35.271Z", Utils.timeStampToStr(ts));
    }

    @Test
    public void groupByTraceId() {
        Map<String, List<Request>> stringListMap = Utils.groupByTraceId(requests1);
        assertEquals(2, stringListMap.size());
        assertEquals(3, stringListMap.get("t1").size());
        assertEquals(1, stringListMap.get("t2").size());
    }

    @Test
    public void mergeTraceIdGroups() {
        List<Map<String, List<Request>>> masterList = new ArrayList<>();
        Map<String, List<Request>> stringListMap = Utils.groupByTraceId(requests1);
        masterList.add(stringListMap);
        stringListMap = Utils.groupByTraceId(requests2);
        masterList.add(stringListMap);

        Map<String, List<Request>> finalTraceRequestMap = Utils.mergeTraceIdGroups(masterList);
        assertEquals(3, finalTraceRequestMap.size());
        assertEquals(3, finalTraceRequestMap.get("t1").size());
        assertEquals(3, finalTraceRequestMap.get("t3").size());
        assertEquals(2, finalTraceRequestMap.get("t2").size());
    }

    @Test
    public void findRoot() {
        Request root = Utils.findRoot(requestsWithNull);
        assertEquals(root.getPrevSpan(), "null");  // It has to have oldspan as null
    }

    @Test
    public void findChildren() {
        Request root = new Request(Timestamp.valueOf("2013-10-23 10:12:35.271"), Timestamp.valueOf("2013-10-23 10:12:35.271"), "t3", "s3", "null", "span3");
        List<Request> children = Utils.findChildren(root, requestsWithNull);
        assertEquals(children.size(), 2);
        assertEquals(children.get(0).getStartTime(), Timestamp.valueOf("2013-10-23 10:12:35.371"));
        assertEquals(children.get(1).getStartTime(), Timestamp.valueOf("2013-10-23 10:12:35.471"));
    }

    @Test
    public void traceMapToTraceObj() {
        // makeRoot also gets tested in this one
        Trace trace = Utils.traceMapToTraceObj("t3", requestsWithNull);
        assertEquals(trace.getId(), "t3");
        assertEquals(trace.getRoot().getService(), "s3");
        assertEquals(trace.getRoot().getStart(), "2013-10-23T10:12:35.271Z");
        assertEquals(trace.getRoot().getEnd(), "2013-10-23T10:12:35.971Z");
        assertEquals(trace.getRoot().getCalls().size(), 2);
        assertEquals(trace.getRoot().getCalls().get(1).getCalls().size(), 0);  // The second element will not have any calls
    }
}
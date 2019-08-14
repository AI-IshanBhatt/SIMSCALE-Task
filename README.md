# simscale-task

Case - Trace reconstruction from logs

Each application in a microservice environment outputs some log describing the boundaries of an HTTP request, with the following format:

[start-timestamp] [end-timestamp] [trace] [service-name] [caller-span]->[span]

The trace ID is a random string that is passed along every service interaction. The first service (called from outside) generates the string and passes it to every other service it calls during the execution of the request. The called services take the trace (let’s say, from an HTTP header) and also pass it to the services the call themselves.

The span ID is generated for every request. When a service calls another, it passes its own span ID to the callee. The callee will generate its own span ID, and using the span passed by the caller, log the last part of the line, that allows to connect the requests.

So, a trace could look like this:

2016-10-20 12:43:34.000 2016-10-20 12:43:35.000 trace1 back-end-3 ac->ad
2016-10-20 12:43:33.000 2016-10-20 12:43:36.000 trace1 back-end-1 aa->ac
2016-10-20 12:43:38.000 2016-10-20 12:43:40.000 trace1 back-end-2 aa->ab
2016-10-20 12:43:32.000 2016-10-20 12:43:42.000 trace1 front-end null->aa

Meaning that the “front-end” received a call from the outside (“null”), and assigned the span “aa” to the request. Then it called back-end-1, who assigned the span “ac”, who in turn called service “back-end-3”, who assigned span “ad”. Then, “front-end” called “back-end-2”.

The entries are logged when the request finishes (as they contain the finishing time), so they are not in calling order, but in finishing order. 

Logs can be mixed up a bit (just because enforcing FIFO semantics is hard in distributed setups), but it is expected that the vast majority are only off for a few milliseconds. 

Timestamps are in UTC.

This execution trace can then be represented as:

{“id: “trace1”,
“root”: {
“service”: “front-end”,
“start”: “2016-10-20 12:43:32.000”,
“end”: “2016-10-20 12:43:42.000”,
“calls”: [
{“service”: “back-end-1”,
“start”: “2016-10-20 12:43:33.000”,
“end”: “2016-10-20 12:43:36.000”,
“calls”: [
{“service”: “back-end-3”,
“start”: “2016-10-20 12:43:34.000”,
“end”: “2016-10-20 12:43:35.000”}]},
{“service”, “back-end-2”,
“start”: “2016-10-20 12:43:38.000”,
“end”: “2016-10-20 12:43:40.000”}
]}}

The task is to produce these JSON trees. That is, given a sequence of log entries, output a JSON for each trace. We should imagine that this application could be deployed as part of some pipeline that starts at the source of the data and ends in some other monitoring application, that presents a stream of recent traces.

Details:
-	The solution should be a Java, Scala or Go program, executable from the command line.
-	The input should be read from standard input or a file (chooseable by the user)
-	The output should be one JSON per line, written to standard output, or a file (chooseable by the user).
-	As said, there can be lines out of order.
-	There can be orphan lines (i.e., services with no corresponding root service); they should be tolerated but not included in the output (maybe summarized in stats, see below).
-	Lines can be malformed, they should be tolerated and ignored.

Features:
-	A nice command-line interface, that allows to specify inputs and outputs from and to files.
-	Optionally report to standard error (or to a file), statistics about progress, lines consumed, line consumption rate, buffers, etc. Both at the end of the processing and during it.
-	Optionally report to standard error (or to a file), statistics about the traces themselves, like number of orphan requests, average size of traces, average depth, etc.
-	As the file could be quite big, try to do the processing using as many cores as the computer has, but only if the processing is actually speeded that way.
Bonus
As the log entries can be (a bit) out of order, and there could be orphans, managing “pending” entries in memory can be important. This would be the case if we indent this to be a long running program (which is not the case in this particular exercise, but is the suggested situation).
As a bonus, you should implement some management of “pending” entries, based on the date of the logs and an “expiration”. As log entries arrive, seeing one with some timestamp would mean that any pending log which is older than some configurable expiration threshold (relative to that timestamp) should be declared orphan (even if its family comes afterwards). This works in the assumption that traces can be a bit delayed, but not come from the future: we are assuming that the timestamp that we receive is, at the most pessimistic case, current.
Supplied
●	Two pairs of files with log and traces. One small to start and one bigger to test more thoroughly.
●	Since there is no deterministic ordering for the traces/calls, it is not enough to just compare two files to know if a solution is correct. For this reason a program (“trace-comparator.jar”, an executable jar file) is supplied for your convenience: it reports if two trace files are the same, regardless of the order. It also parses the traces, which also allows to check if the format is correct. This is just for convenience, to compare a potential solution with the supplied references.



package phd.research.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Tuple;
import phd.research.core.DroidGraph;
import phd.research.graph.Control;
import phd.research.singletons.FlowDroidAnalysis;
import phd.research.vertices.ControlVertex;
import phd.research.vertices.Vertex;
import phd.research.vertices.VertexFactory;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jordan Doyle
 */

public class LogHandler {

    public static final String M_TAG = "<METHOD>";
    public static final String I_TAG = "<INTERACTION>";
    public static final String C_TAG = "<CONTROL>";

    private static final Logger LOGGER = LoggerFactory.getLogger(LogHandler.class);

    public static Tuple<Integer, SootClass, SootMethod> regexLogMessage(String logMessage) {
        Matcher matcher;
        Pattern pattern;

        if (logMessage.contains(C_TAG)) {
            pattern = Pattern.compile("Method:\\s<(.+):\\s(.+)>\\sView:\\s(-?\\d+)");
        } else if (logMessage.contains(M_TAG)) {
            pattern = Pattern.compile("Method:\\s<(.+):\\s(.+)>");
        } else {
            LOGGER.error("No recognisable tag in log message: " + logMessage);
            return null;
        }

        if (logMessage.contains("'")) {
            logMessage = logMessage.replaceAll("'", "");
            LOGGER.warn("Removed single quotation from log message.");
        }

        if (!FlowDroidAnalysis.v().isSootInitialised()) {
            FlowDroidAnalysis.v().initializeSoot();
        }

        matcher = pattern.matcher(logMessage);
        if (matcher.find()) {
            SootClass sootClass = Scene.v().getSootClassUnsafe(matcher.group(1));
            if (sootClass != null) {
                SootMethod method = sootClass.getMethodUnsafe(matcher.group(2));
                if (sootClass.hasOuterClass()) {
                    sootClass = sootClass.getOuterClass();
                    if (method == null) {
                        method = sootClass.getMethodUnsafe(matcher.group(2));
                    }
                }

                if (method != null) {
                    if (logMessage.contains(C_TAG)) {
                        Integer id = Integer.parseInt(matcher.group(3));
                        return new Tuple<>(id, sootClass, method);
                    } else if (logMessage.contains(M_TAG)) {
                        return new Tuple<>(-1, sootClass, method);
                    }
                } else {
                    LOGGER.error(
                            String.format("Failed to retrieve class %s found in Logcat message.", matcher.group(1)));
                }
            } else {
                LOGGER.error(String.format("Failed to retrieve method %s found in Logcat message.", matcher.group(2)));
            }
        }

        LOGGER.error("Failure while reading Logcat message: " + logMessage);
        return null;
    }

    public static void handleInstrumentLog(String logMessage, DroidGraph droidGraph) {
        Tuple<Integer, SootClass, SootMethod> logDataGroup = regexLogMessage(logMessage);

        if (logDataGroup != null) {
            LOGGER.debug("Interaction/Method: " + logMessage);

            Vertex v = DroidGraph.getMethodVertex(logDataGroup.getRight().getSignature(),
                    droidGraph.getControlFlowGraph().vertexSet()
                                                 );
            if (v == null) {
                LOGGER.error("Failed to visit method: " + logDataGroup.getRight());
            } else {
                v.visit();
            }

            if (logDataGroup.getLeft() != -1) {
                Vertex viewVertex =
                        DroidGraph.getControlVertex(logDataGroup.getMiddle().getName(), logDataGroup.getLeft(),
                                droidGraph.getControlFlowGraph().vertexSet()
                                                   );
                if (viewVertex == null) {
                    LOGGER.warn("Failed to visit view vertex: " + logDataGroup.getLeft());
                } else {
                    viewVertex.visit();
                }
            }
        }
    }

    public static void updateGraph(String logMessage, DroidGraph droidGraph) {
        Tuple<Integer, SootClass, SootMethod> logDataGroup = regexLogMessage(logMessage);

        if (logDataGroup != null) {
            LOGGER.debug("Interaction/Method: " + logMessage);

            Vertex methodVertex = DroidGraph.getMethodVertex(logDataGroup.getRight().getSignature(),
                    droidGraph.getControlFlowGraph().vertexSet()
                                                            );
            if (methodVertex == null) {
                LOGGER.error("Failed to visit method vertex: " + logDataGroup.getRight());
                LOGGER.info("Adding method vertex with signature: " + logDataGroup.getRight());
                VertexFactory factory = new VertexFactory();
                methodVertex = factory.createVertex(logDataGroup.getRight());
                droidGraph.getControlFlowGraph().addVertex(methodVertex);
            }

            if (logDataGroup.getLeft() != -1) {
                Vertex controlVertex =
                        DroidGraph.getControlVertex(logDataGroup.getMiddle().getName(), logDataGroup.getLeft(),
                                droidGraph.getControlFlowGraph().vertexSet()
                                                   );
                if (controlVertex == null) {
                    LOGGER.warn("Failed to visit control vertex: " + logDataGroup.getLeft());
                    LOGGER.info("Adding control vertex with Id: " + logDataGroup.getLeft());
                    controlVertex = new ControlVertex(new Control(logDataGroup.getLeft(), "Unknown", -1, "Unknown",
                            logDataGroup.getMiddle().getName(), Collections.emptyList()
                    ));
                    droidGraph.getControlFlowGraph().addVertex(controlVertex);
                }
            }
        }
    }

    public static void updateGraphEdges(String logMessage, DroidGraph droidGraph) {
        Tuple<Integer, SootClass, SootMethod> logDataGroup = regexLogMessage(logMessage);

        if (logDataGroup != null) {
            LOGGER.debug("Interaction/Method: " + logMessage);

            Vertex methodVertex = DroidGraph.getMethodVertex(logDataGroup.getRight().getSignature(),
                    droidGraph.getControlFlowGraph().vertexSet()
                                                            );
            if (methodVertex == null) {
                LOGGER.error("Failed to find method vertex: " + logDataGroup.getRight());
                LOGGER.error("Failed to add edge to method vertex: " + logDataGroup.getRight());
                return;
            }

            if (logDataGroup.getLeft() == -1) {
                LOGGER.error("Failed to add edge to listener vertex: " + logDataGroup.getLeft());
                return;
            }

            if (logDataGroup.getLeft() != -1) {
                Vertex controlVertex =
                        DroidGraph.getControlVertex(logDataGroup.getMiddle().getName(), logDataGroup.getLeft(),
                                droidGraph.getControlFlowGraph().vertexSet()
                                                   );
                if (controlVertex == null) {
                    LOGGER.warn("Failed to find control vertex: " + logDataGroup.getLeft());
                    LOGGER.error("Failed to add edge to listener vertex: " + logDataGroup.getLeft());
                }

                droidGraph.getControlFlowGraph().addEdge(controlVertex, methodVertex);
            }
        }
    }
}

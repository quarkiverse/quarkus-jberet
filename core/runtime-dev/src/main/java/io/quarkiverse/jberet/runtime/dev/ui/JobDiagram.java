package io.quarkiverse.jberet.runtime.dev.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jberet.job.model.Decision;
import org.jberet.job.model.Flow;
import org.jberet.job.model.Job;
import org.jberet.job.model.JobElement;
import org.jberet.job.model.RefArtifact;
import org.jberet.job.model.Split;
import org.jberet.job.model.Step;
import org.jberet.job.model.Transition;

/**
 * Generates a Graphviz DOT diagram of the flow of a {@link Job}. Steps and decisions are nodes, flows and splits are
 * clusters, and <code>next</code>, <code>end</code>, <code>fail</code> and <code>stop</code> transitions are edges.
 * <p>
 * If the status of each step is known (from a job execution), the steps are colored by their status.
 */
final class JobDiagram {
    private static final String START = "__start";
    private static final String END = "__end";
    private static final String GREY = "#8a8a8a";

    private final Job job;
    private final Map<String, String> stepStatuses;
    private final Map<String, Endpoint> entries = new HashMap<>();
    private final Map<String, Endpoint> exits = new HashMap<>();
    private final StringBuilder dot = new StringBuilder();
    private int terminations;
    private boolean endUsed;

    private JobDiagram(final Job job, final Map<String, String> stepStatuses) {
        this.job = job;
        this.stepStatuses = stepStatuses;
    }

    /**
     * @param job the Job to draw
     * @param stepStatuses the batch status of each step by step name, or <code>null</code> to draw the Job definition
     * @return the Job diagram in Graphviz DOT format
     */
    static String toDot(final Job job, final Map<String, String> stepStatuses) {
        return new JobDiagram(job, stepStatuses).generate();
    }

    private String generate() {
        dot.append("digraph \"").append(escape(job.getId())).append("\" {\n");
        dot.append("  graph [rankdir=LR, compound=true, bgcolor=\"transparent\", fontname=\"Helvetica\", fontsize=11, ")
                .append("fontcolor=\"").append(GREY).append("\", nodesep=0.3, ranksep=0.4];\n");
        dot.append("  node [fontname=\"Helvetica\", fontsize=11, shape=box, style=\"rounded,filled\", ")
                .append("fillcolor=\"#e8eef7\", color=\"#5a7bb5\", fontcolor=\"#1f2937\"];\n");
        dot.append("  edge [fontname=\"Helvetica\", fontsize=9, color=\"").append(GREY)
                .append("\", fontcolor=\"").append(GREY).append("\", arrowsize=0.7];\n");
        dot.append("  \"").append(START).append("\" [shape=circle, label=\"\", width=0.2, style=filled, fillcolor=\"")
                .append(GREY).append("\", color=\"").append(GREY).append("\"];\n");

        List<JobElement> elements = job.getJobElements();
        declare(elements, "  ");
        if (elements.isEmpty()) {
            edge(new Endpoint(START, null), end(), null);
        } else {
            edge(new Endpoint(START, null), entries.get(elements.get(0).getId()), null);
            transitions(elements, true);
        }

        if (endUsed) {
            dot.append("  \"").append(END).append("\" [shape=doublecircle, label=\"\", width=0.12, style=filled, fillcolor=\"")
                    .append(GREY).append("\", color=\"").append(GREY).append("\"];\n");
        }
        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Declares the nodes and clusters of the elements, and records the nodes to use to enter and exit each element.
     */
    private void declare(final List<JobElement> elements, final String indent) {
        for (JobElement element : elements) {
            String id = element.getId();
            if (element instanceof Step step) {
                String node = "step:" + id;
                dot.append(indent).append(quote(node)).append(" [label=<").append(stepLabel(step)).append(">")
                        .append(stepStyle(id)).append("];\n");
                entries.put(id, new Endpoint(node, null));
                exits.put(id, new Endpoint(node, null));
            } else if (element instanceof Decision decision) {
                String node = "decision:" + id;
                dot.append(indent).append(quote(node)).append(" [shape=diamond, style=filled, fillcolor=\"#fdf3d7\", ")
                        .append("color=\"#c9a227\", label=<<b>").append(html(id)).append("</b><br/><font point-size=\"9\">")
                        .append(html(decision.getRef())).append("</font>>];\n");
                entries.put(id, new Endpoint(node, null));
                exits.put(id, new Endpoint(node, null));
            } else if (element instanceof Flow flow) {
                declareFlow(flow, indent);
            } else if (element instanceof Split split) {
                String cluster = "cluster_split:" + id;
                String fork = "fork:" + id;
                String join = "join:" + id;
                dot.append(indent).append("subgraph ").append(quote(cluster)).append(" {\n");
                dot.append(indent).append("  label=\"split: ").append(escape(id))
                        .append("\"; style=\"dashed,rounded\"; color=\"").append(GREY).append("\";\n");
                dot.append(indent).append("  ").append(quote(fork)).append(" [shape=box, label=\"\", width=0.06, height=0.5, ")
                        .append("style=filled, fillcolor=\"").append(GREY).append("\", color=\"").append(GREY).append("\"];\n");
                dot.append(indent).append("  ").append(quote(join)).append(" [shape=box, label=\"\", width=0.06, height=0.5, ")
                        .append("style=filled, fillcolor=\"").append(GREY).append("\", color=\"").append(GREY).append("\"];\n");
                for (Flow flow : split.getFlows()) {
                    declareFlow(flow, indent + "  ");
                }
                dot.append(indent).append("}\n");
                entries.put(id, new Endpoint(fork, null));
                exits.put(id, new Endpoint(join, null));
            }
        }
    }

    private void declareFlow(final Flow flow, final String indent) {
        String id = flow.getId();
        String cluster = "cluster_flow:" + id;
        dot.append(indent).append("subgraph ").append(quote(cluster)).append(" {\n");
        dot.append(indent).append("  label=\"flow: ").append(escape(id))
                .append("\"; style=\"rounded\"; color=\"").append(GREY).append("\";\n");
        List<JobElement> elements = flow.getJobElements();
        if (elements.isEmpty()) {
            String node = "empty:" + id;
            dot.append(indent).append("  ").append(quote(node)).append(" [shape=point, style=invis];\n");
            entries.put(id, new Endpoint(node, cluster));
            exits.put(id, new Endpoint(node, cluster));
        } else {
            declare(elements, indent + "  ");
            // The flow continues after an element without a next element, which is usually the last one
            JobElement last = elements.get(elements.size() - 1);
            for (JobElement element : elements) {
                if (next(element) == null && !hasCatchAll(element)) {
                    last = element;
                }
            }
            entries.put(id, new Endpoint(entries.get(elements.get(0).getId()).node(), cluster));
            exits.put(id, new Endpoint(exits.get(last.getId()).node(), cluster));
        }
        dot.append(indent).append("}\n");
    }

    /**
     * Draws the transitions between the elements. When no transition matches the exit status, and there is no
     * <code>next</code> attribute, a top level element ends the Job, and an element in a flow ends the enclosing flow
     * (drawn as a transition from the flow).
     */
    private void transitions(final List<JobElement> elements, final boolean topLevel) {
        for (JobElement element : elements) {
            Endpoint exit = exits.get(element.getId());
            String next = next(element);
            if (next != null) {
                edge(exit, entries.get(next), null);
            }

            for (Transition<?> transition : element.getTransitionElements()) {
                if (transition instanceof Transition.Next<?> nextTransition) {
                    edge(exit, entries.get(nextTransition.getTo()), transition.getOn());
                } else if (transition instanceof Transition.Termination<?> termination) {
                    edge(exit, termination(termination), transition.getOn());
                }
            }

            if (topLevel && next == null && !hasCatchAll(element)) {
                edge(exit, end(), null);
            }

            if (element instanceof Flow flow) {
                transitions(flow.getJobElements(), false);
            } else if (element instanceof Split split) {
                Endpoint fork = entries.get(split.getId());
                Endpoint join = exits.get(split.getId());
                for (Flow flow : split.getFlows()) {
                    edge(fork, entries.get(flow.getId()), null);
                    edge(exits.get(flow.getId()), join, null);
                    transitions(flow.getJobElements(), false);
                }
            }
        }
    }

    private Endpoint end() {
        endUsed = true;
        return new Endpoint(END, null);
    }

    private static boolean hasCatchAll(final JobElement element) {
        for (Transition<?> transition : element.getTransitionElements()) {
            if ("*".equals(transition.getOn())) {
                return true;
            }
        }
        return false;
    }

    private Endpoint termination(final Transition.Termination<?> termination) {
        String node = "termination:" + terminations++;
        String label;
        String color;
        String shape;
        if (termination instanceof Transition.Fail<?>) {
            label = "FAIL";
            color = "#d0312d";
            shape = "octagon";
        } else if (termination instanceof Transition.Stop<?> stop) {
            label = "STOP" + (stop.getRestart() != null ? "<br/><font point-size=\"9\">restart: " + html(stop.getRestart())
                    + "</font>" : "");
            color = "#d98a00";
            shape = "octagon";
        } else {
            label = "END";
            color = GREY;
            shape = "box";
        }
        if (termination.getExitStatus() != null) {
            label += "<br/><font point-size=\"9\">exit: " + html(termination.getExitStatus()) + "</font>";
        }
        dot.append("  ").append(quote(node)).append(" [shape=").append(shape).append(", style=\"rounded,filled\", ")
                .append("fillcolor=\"white\", color=\"").append(color).append("\", fontcolor=\"").append(color)
                .append("\", fontsize=9, label=<").append(label).append(">];\n");
        return new Endpoint(node, null);
    }

    private void edge(final Endpoint from, final Endpoint to, final String label) {
        if (from == null || to == null) {
            // Transition to an unknown element. The Job is invalid, but there is no point in failing the diagram
            return;
        }
        dot.append("  ").append(quote(from.node())).append(" -> ").append(quote(to.node()));
        StringBuilder attributes = new StringBuilder();
        if (from.cluster() != null) {
            attributes.append("ltail=").append(quote(from.cluster())).append(", ");
        }
        if (to.cluster() != null) {
            attributes.append("lhead=").append(quote(to.cluster())).append(", ");
        }
        if (label != null) {
            attributes.append("label=").append(quote(label)).append(", ");
        }
        if (!attributes.isEmpty()) {
            attributes.setLength(attributes.length() - 2);
            dot.append(" [").append(attributes).append("]");
        }
        dot.append(";\n");
    }

    private static String stepLabel(final Step step) {
        StringBuilder label = new StringBuilder();
        label.append("<b>").append(html(step.getId())).append("</b>");
        if (step.getBatchlet() != null) {
            label.append("<br/><font point-size=\"9\">batchlet: ").append(html(ref(step.getBatchlet()))).append("</font>");
        } else if (step.getChunk() != null) {
            label.append("<br/><font point-size=\"9\">");
            label.append(html(ref(step.getChunk().getReader())));
            if (step.getChunk().getProcessor() != null) {
                label.append(" → ").append(html(ref(step.getChunk().getProcessor())));
            }
            label.append(" → ").append(html(ref(step.getChunk().getWriter())));
            label.append("</font>");
        }
        if (step.getPartition() != null) {
            label.append("<br/><font point-size=\"9\"><i>partitioned</i></font>");
        }
        return label.toString();
    }

    private String stepStyle(final String stepName) {
        if (stepStatuses == null) {
            return "";
        }
        String status = stepStatuses.get(stepName);
        if (status == null) {
            // Not executed
            return ", fillcolor=\"#f3f4f6\", color=\"#b0b0b0\", fontcolor=\"#6b7280\", style=\"rounded,filled,dashed\"";
        }
        return switch (status) {
            case "COMPLETED" -> ", fillcolor=\"#d1f2d9\", color=\"#2e9d4f\"";
            case "FAILED" -> ", fillcolor=\"#f9d6d5\", color=\"#d0312d\", penwidth=2";
            case "STOPPED", "STOPPING" -> ", fillcolor=\"#fde9c8\", color=\"#d98a00\"";
            case "STARTED", "STARTING" -> ", fillcolor=\"#d6e6fb\", color=\"#2f6fd0\", penwidth=2";
            default -> ", fillcolor=\"#e5e7eb\", color=\"#6b7280\"";
        };
    }

    private static String next(final JobElement element) {
        if (element instanceof Step step) {
            return step.getAttributeNext();
        } else if (element instanceof Flow flow) {
            return flow.getAttributeNext();
        } else if (element instanceof Split split) {
            return split.getAttributeNext();
        }
        return null;
    }

    private static String ref(final RefArtifact refArtifact) {
        return refArtifact != null && refArtifact.getRef() != null ? refArtifact.getRef() : "?";
    }

    private static String quote(final String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String escape(final String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String html(final String value) {
        return value == null ? ""
                : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * A node to connect an edge to. If the node is inside a cluster that represents the element, the edge is clipped at
     * the cluster boundary.
     */
    private record Endpoint(String node, String cluster) {
    }
}

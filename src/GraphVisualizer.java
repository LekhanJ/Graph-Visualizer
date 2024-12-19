import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GraphVisualizer extends JFrame {
    private List<Node> nodes; // Stores all the nodes
    private List<Edge> edges; // Stores all the edges
    private Node selectedNode; // Keeps track of the node on which we click
    private Node sourceNode; // Keeps track of the source node of edge
    private Point dragPoint; // Stores the current mouse position during dragging
    private Point offset;  // Store click offset from node center
    private int nodeRadius = 25;
    private int nodeDetectionRadius = 30;
    private DrawPanel drawPanel;
    private boolean isCreatingEdge = false;  // Added to differentiate between moving and edge creation

    public GraphVisualizer() {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();

        setTitle("Graph Visualizer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawPanel = new DrawPanel();
        add(drawPanel);

        drawPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Node clickedNode = findNode(e.getX(), e.getY());
                if (clickedNode != null) {
                    selectedNode = clickedNode;
                    // Calculate offset from node center to click point
                    offset = new Point(e.getX() - clickedNode.x, e.getY() - clickedNode.y);

                    // Check if right mouse button (edge creation)
                    if (SwingUtilities.isRightMouseButton(e)) {
                        isCreatingEdge = true;
                        sourceNode = clickedNode;
                        dragPoint = new Point(e.getX(), e.getY());
                    } else {
                        isCreatingEdge = false;
                    }
                } else {
                    // Create new node at click position
                    Node newNode = new Node(e.getX(), e.getY(), "Node " + nodes.size());
                    nodes.add(newNode);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isCreatingEdge && sourceNode != null) {
                    Node targetNode = findNode(e.getX(), e.getY());

                    if (targetNode != null && targetNode != sourceNode) {
                        int distance = (int)Math.sqrt(Math.pow(targetNode.x - sourceNode.x, 2) + Math.pow(targetNode.y - sourceNode.y, 2));
                        edges.add(new Edge(sourceNode, targetNode, distance));
                        System.out.println("Edge created between " + sourceNode.label + " and " + targetNode.label);
                    }
                }
                // Reset all temporary variables
                selectedNode = null;
                sourceNode = null;
                dragPoint = null;
                offset = null;
                isCreatingEdge = false;

                // Remove highlights from all nodes
                for (Node node : nodes) {
                    node.isHighlighted = false;
                }
                repaint();
            }
        });

        drawPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedNode != null) {
                    if (isCreatingEdge) {
                        // Update drag point for edge creation
                        dragPoint = new Point(e.getX(), e.getY());

                        // Highlight potential target node
                        Node potentialTarget = findNode(e.getX(), e.getY());
                        for (Node node : nodes) {
                            node.isHighlighted = (node == potentialTarget && node != sourceNode);
                        }
                    } else {
                        // Move the node, accounting for the initial offset
                        selectedNode.x = e.getX() - offset.x;
                        selectedNode.y = e.getY() - offset.y;

                        // Update distances for all edges connected to this node
                        for (Edge edge : edges) {
                            if (edge.source == selectedNode || edge.target == selectedNode) {
                                edge.distance = calculateDistance(edge.source, edge.target);
                            }
                        }
                    }
                    repaint();
                }
            }
        });

        // Add instruction label
        JLabel instructionLabel = new JLabel(
                "<html>Instructions:<br>" +
                        "• Left click anywhere to create a node<br>" +
                        "• Left click and drag a node to move it<br>" +
                        "• Right click and drag from one node to another to create an edge</html>"
        );
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(instructionLabel, BorderLayout.NORTH);
    }

    private Node findNode(int x, int y) {
        for (Node node : nodes) {
            double distance = Math.sqrt(Math.pow(node.x - x, 2) + Math.pow(node.y - y, 2));
            if (distance < nodeDetectionRadius) {
                return node;
            }
        }
        return null;
    }

    private int calculateDistance(Node source, Node target) {
        return (int) Math.sqrt(
                Math.pow(target.x - source.x, 2) +
                        Math.pow(target.y - source.y, 2)
        ) - 52;
    }

    private class DrawPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw edges
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            for (Edge edge : edges) {
                // Draw the line
                g2d.drawLine(edge.source.x, edge.source.y, edge.target.x, edge.target.y);

                // Calculate the midpoint of the edge for label placement
                int midX = (edge.source.x + edge.target.x) / 2;
                int midY = (edge.source.y + edge.target.y) / 2;

                // Create a background for the text
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(Integer.toString(edge.distance));
                int textHeight = fm.getHeight();
                int padding = 4;

                g2d.setColor(Color.WHITE);
                g2d.fillRect(midX - textWidth/2 - padding,
                        midY - textHeight/2,
                        textWidth + 2*padding,
                        textHeight);

                // Draw the edge label
                g2d.setColor(Color.BLACK);
                g2d.drawString(Integer.toString(edge.distance),
                        midX - textWidth/2,
                        midY + fm.getAscent()/2);
            }

            // Draw temporary line while dragging for edge creation
            if (isCreatingEdge && sourceNode != null && dragPoint != null) {
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
                g2d.drawLine(sourceNode.x, sourceNode.y, dragPoint.x, dragPoint.y);
            }

            // Draw nodes
            for (Node node : nodes) {
                // Draw node background
                if (node.isHighlighted) {
                    g2d.setColor(Color.YELLOW);
                } else if (node == selectedNode) {
                    g2d.setColor(Color.LIGHT_GRAY);
                } else {
                    g2d.setColor(Color.WHITE);
                }
                g2d.fillOval(node.x - nodeRadius, node.y - nodeRadius,
                        nodeRadius * 2, nodeRadius * 2);

                // Draw node border
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(node.x - nodeRadius, node.y - nodeRadius,
                        nodeRadius * 2, nodeRadius * 2);

                // Draw node label
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(node.label);
                g2d.drawString(node.label,
                        node.x - textWidth / 2,
                        node.y + fm.getAscent() / 2);
            }
        }
    }

    private static class Node {
        int x, y;
        String label;
        boolean isHighlighted;

        Node(int x, int y, String label) {
            this.x = x;
            this.y = y;
            this.label = label;
            this.isHighlighted = false;
        }
    }

    private static class Edge {
        Node source;
        Node target;
        int distance;

        Edge(Node source, Node target, int distance) {
            this.source = source;
            this.target = target;
            this.distance = distance;
        }
    }
}
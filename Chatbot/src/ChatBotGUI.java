import com.google.gson.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ChatBotGUI extends JFrame {
    private JPanel chatPanel;
    private JTextField userInput;
    private JButton sendButton;
    private JScrollPane scrollPane;

    // Your API key
    private final String apiKey = "AIzaSyCwTZSYJCLvBtCU4QYYGkvfHQVdiC9M2tQ";  // 🔐 Replace with your Gemini API key
    private String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent?key=" + apiKey;

    // Holds conversation history (only to display previous messages in chat)
    private final List<Map<String, Object>> contents = new ArrayList<>();

    public ChatBotGUI() {
        setTitle("Gemini Chatbot ✨");
        setSize(500, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Set up chat panel
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));  // Stack messages vertically
        chatPanel.setBackground(new Color(240, 240, 240));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Set up bottom panel for user input
        JPanel bottomPanel = new JPanel(new BorderLayout());
        userInput = new JTextField();
        userInput.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(25, 135, 84));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));

        bottomPanel.add(userInput, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        userInput.addActionListener(e -> sendMessage());

        setVisible(true);
    }

    private void sendMessage() {
        String userText = userInput.getText().trim();
        if (userText.isEmpty()) return;

        appendToChat("You: " + userText, true);
        userInput.setText("");  // Clear the input field

        // Add user message to history for display purposes
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userText))
        ));

        new Thread(() -> {
            try {
                String reply = getGeminiResponse(userText); // Get response for the current message only
                appendToChat("Gemini: " + reply, false);

                // Add bot's response to history
                contents.add(Map.of(
                        "role", "model",
                        "parts", List.of(Map.of("text", reply))
                ));
            } catch (Exception e) {
                appendToChat("Gemini: [Error getting response]", false);
                e.printStackTrace();
            }
        }).start();
    }

    private void appendToChat(String message, boolean isUser) {
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(new Color(245, 245, 245));
        messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        messageArea.setOpaque(true);

        if (isUser) {
            messageArea.setBackground(new Color(220, 248, 198));  // User messages are light green
        } else {
            messageArea.setBackground(new Color(225, 225, 255));  // Bot messages are light blue
        }

        // Add message to chat panel and scroll to the latest message
        chatPanel.add(Box.createVerticalStrut(10));
        chatPanel.add(messageArea);
        chatPanel.add(Box.createVerticalStrut(10));

        // Scroll to the bottom
        JScrollPane scrollPane = (JScrollPane) getContentPane().getComponent(0);  // Get the JScrollPane
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());

        revalidate();
        repaint();
    }

    private String getGeminiResponse(String userInput) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        // Prepare request body
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", userInput)));
        content.put("role", "user");

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        String json = new Gson().toJson(body);

        OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
        writer.write(json);
        writer.flush();
        writer.close();

        // Get response from the server
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                con.getResponseCode() == 200 ? con.getInputStream() : con.getErrorStream()
        ));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        reader.close();

        String rawResponse = responseBuilder.toString();
        System.out.println("Raw Response: " + rawResponse);  // Print raw API response

        try {
            // Extract the response
            JsonObject response = JsonParser.parseString(rawResponse).getAsJsonObject();
            return response
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString().trim();
        } catch (Exception e) {
            return "[Invalid response from Gemini API]";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatBotGUI::new);
    }
}

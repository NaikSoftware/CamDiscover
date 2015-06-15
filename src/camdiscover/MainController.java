/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package camdiscover;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * FXML Controller class
 *
 * @author Naik
 */
public class MainController implements Initializable {

    @FXML
    private TextField tfFirstIP;
    @FXML
    private TextField tfSecondIP;
    @FXML
    private TextField tfRangeFromIP;
    @FXML
    private TextField tfRangeToIP;
    @FXML
    private TextField tfTimeout;
    @FXML
    private TextField tfMaxThreads;
    @FXML
    private CheckBox chBoxAnyServers;
    @FXML
    private ListView<String> listView;
    @FXML
    private Button btnDiscover;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextField tfPorts;

    private boolean running;
    private boolean anyServers;
    private CamDiscover main;

    private int connectTimeout = 250;
    private final List<String> searchHeaders = new ArrayList<String>() {
        {
            add("DNVRS-Webs");
            add("DVRDVS-Webs");
            add("Hikvision-Webs");
            add("App-webs");
            add("Netwave IP Camera");
            add("Webcam");
            add("WebCam");
            add("webcam");
        }
    };
    private int threads = 25;
    private String urlPrefix;
    private int[] range = {0, 255};
    private ExecutorService pool;
    private int requestsInThread = 32;
    private final AtomicInteger mainCounter = new AtomicInteger(0);
    private int requests;
    private String[] ports;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setNumberFormat(tfFirstIP);
        setNumberFormat(tfMaxThreads);
        setNumberFormat(tfRangeFromIP);
        setNumberFormat(tfRangeToIP);
        setNumberFormat(tfSecondIP);
        setNumberFormat(tfTimeout);
        btnDiscover.setOnAction((ActionEvent) -> {
            if (!running) {
                btnDiscover.setText("Force stop");
                String s1 = tfFirstIP.getText();
                if (s1.isEmpty()) {
                    s1 = "0";
                }
                String s2 = tfSecondIP.getText();
                if (s2.isEmpty()) {
                    s2 = "0";
                }
                urlPrefix = s1 + "." + s2 + ".";
                s1 = tfRangeFromIP.getText();
                if (s1.isEmpty()) {
                    s1 = "0";
                }
                s2 = tfRangeToIP.getText();
                if (s2.isEmpty()) {
                    s2 = "0";
                }
                range[0] = Integer.parseInt(s1);
                range[1] = Integer.parseInt(s2);
                s1 = tfMaxThreads.getText();
                if (!s1.isEmpty()) {
                    threads = Integer.parseInt(s1);
                }
                s1 = tfTimeout.getText();
                if (!s1.isEmpty()) {
                    connectTimeout = Integer.parseInt(s1);
                }
                s1 = tfPorts.getText();
                if (!s1.isEmpty()) {
                    ports = s1.split("\\s+");
                } else {
                    ports = new String[]{"80"};
                }
                start();
            } else {
                pool.shutdownNow();
                btnDiscover.setText("Discover");
            }
            running = !running;
        });
        listView.setItems(FXCollections.observableArrayList());
        listView.setOnMouseClicked((MouseEvent ev) -> {
            if (ev.getButton().equals(MouseButton.PRIMARY) && ev.getClickCount() == 2) {
                String url_str = listView.getSelectionModel().getSelectedItems().get(0);
                openBrowser(url_str);
            }
        });
        chBoxAnyServers.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            anyServers = newValue;
        });
    }

    private void start() {
        listView.getItems().clear();
        mainCounter.set(0);
        requests = ((range[1] - range[0] + 1) * 256);
        pool = Executors.newFixedThreadPool(threads);
        for (int i = range[0]; i <= range[1]; i++) {
            for (int j = 0; j < 256; j++) {
                if (j % requestsInThread == 0) {;
                    pool.submit(new DiscoverTask(i, j));;
                }
            }
        }
        pool.shutdown();
    }

    private class DiscoverTask implements Runnable {

        private int third, fromFifth;

        public DiscoverTask(int third, int fromFifth) {
            this.third = third;
            this.fromFifth = fromFifth;
        }

        public void run() {
            int num = 0;
            String server;
            for (int i = 0; i <= requestsInThread && fromFifth + i < 256; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                String address = urlPrefix + third + "." + (fromFifth + i);
                String httpAddr = "http://" + address;
                for (int p = 0; p < ports.length; p++) {
                    int port = Integer.parseInt(ports[p]);
                    if (port == 80) {
                        HttpURLConnection connect = null;
                        try {
                            URL url = new URL(httpAddr);
                            connect = (HttpURLConnection) url.openConnection();
                            connect.setReadTimeout(connectTimeout);
                            connect.setConnectTimeout(connectTimeout);
                            connect.connect();
                            server = connect.getHeaderField("Server");

                            if (server != null) {
                                if (anyServers) {
                                    addLink(address + " " + server);
                                } else if (searchHeaders.contains(server)) {
                                    addLink(address + " " + server);
                                }
                            } else {
                                server = connect.getHeaderField("WWW-Authenticate");
                                if (server != null) {
                                    if (anyServers) {
                                        addLink(address + " WWW-Authenticate");
                                    } else if (searchHeaders.contains(server)) {
                                        addLink(address + " WWW-Authenticate  webcam");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            //e.printStackTrace();
                        } finally {
                            connect.disconnect();
                        }
                    } else { // Other port, scan via socket
                        //address = "193.189.127.180";
                        try {
                            InetSocketAddress addr = new InetSocketAddress(address, port);
                            Socket sock = new Socket();
                            sock.connect(addr, connectTimeout);
                            InputStream in = sock.getInputStream();
                            addLink(address + ":" + port + readFromSocket(sock));
                            sock.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                num = mainCounter.incrementAndGet();
                progressBar.setProgress((float) num / requests);
                if (num == requests) {
                    Platform.runLater(() -> {
                        btnDiscover.setText("Discover");//end
                        running = false;
                    });
                }
            }
        }

    }

    private void addLink(String url) {
        Platform.runLater(() -> {
            listView.getItems().add(url);
        });
    }

    private void openBrowser(final String url) {
        main.getHostServices().showDocument(url.substring(0, url.indexOf(' ')));
    }

    private static String readFromSocket(Socket sock) throws Exception {
        InputStream in = sock.getInputStream();
        StringBuilder result = new StringBuilder(" ");
        int timeout = 0;
        int a;
        while (true) {
            a = in.available();
            if (a > 0) {
                int r = in.read();
                if (r == -1) {
                    break;
                }
                result.append((char) r);
                timeout = 0;
            } else {
                Thread.sleep(10);
                a = in.available();
                if (a == 0) {
                    timeout++;
                }
                if (timeout > 40) {
                    break;  // timeout
                }
            }
        }
        return result.toString();
    }

    private void setNumberFormat(TextField tf) {
        tf.addEventFilter(KeyEvent.KEY_TYPED, (KeyEvent inputevent) -> {
            if (!inputevent.getCharacter().matches("\\d")) {
                inputevent.consume();
            }
        });
    }

    public void setMainClass(CamDiscover main) {
        this.main = main;
    }

}

package no.softwarecontrol.idoc.web.signup;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.*;

@WebListener
public class SignupDispatcher implements ServletContextListener {

    private static final int PROCESSING_THREAD_COUNT = 2;
    private static final BlockingQueue<SignupClient> REMOTE_CLIENTS = new LinkedBlockingQueue<SignupClient>();
    private final ExecutorService executor = Executors.newFixedThreadPool(PROCESSING_THREAD_COUNT);
    private int currentTaskCounter = 0;
    private int totalTaskCounter = 0;

    public static void addRemoteClient(SignupClient signupClient) {
        REMOTE_CLIENTS.add(signupClient);
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        int count = 0;
        while (count < PROCESSING_THREAD_COUNT) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    String threadName = Thread.currentThread().getName();
                    while (true) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        SignupClient remoteClient;

                        try {
                            String beat = "";
                            for (int i = 0; i < 8192; i++) {
                                beat = beat + "x";
                            }
                            final String heartBeat = beat;

                            // fetch a remote client from the waiting queue
                            // (this call blocks until a client is available)
                            remoteClient = REMOTE_CLIENTS.take();

                            AsyncContext asyncContext = remoteClient.getAsyncContext();
                            ServletResponse response = asyncContext.getResponse();
                            final PrintWriter out = response.getWriter();
                            currentTaskCounter = 0;

                            SignupTask signupTask = new SignupTask(remoteClient.getCustomerData());
                            totalTaskCounter = signupTask.getTaskCount();
                            signupTask.addListener(new SignupTask.SignupTaskListener() {
                                @Override
                                public void onProgress() {
                                    out.print(heartBeat);
                                }

                                @Override
                                public void onFinished() {
                                    out.flush();
                                    out.close();
                                }
                            });

                            response.setContentLength(totalTaskCounter * heartBeat.length());
                            response.setContentType("text/plain");

                            signupTask.execute();
                        } catch (InterruptedException e1) {
                            throw new RuntimeException("Interrupted while waiting for remote clients");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            count++;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        System.out.println("Shutting down SignupDispatchers");
        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("tasks interrupted");
        } finally {
            if (!executor.isTerminated()) {
                System.err.println("cancel non-finished tasks");
            }
            executor.shutdownNow();
        }
        REMOTE_CLIENTS.clear();
    }
}

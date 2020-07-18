package com.tuiasi.central_module.threading;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class NotifyingThread extends Thread {
    private final Set<ThreadListener> threadListeners
            = new CopyOnWriteArraySet<>();

    private final void notifyListeners(boolean finishedSuccessful) {
        for (ThreadListener listener : threadListeners) {
            listener.onThreadComplete(this, finishedSuccessful);
        }
    }

    public final void addListener(final ThreadListener listener) {
        threadListeners.add(listener);
    }

    public final void removeListener(final ThreadListener listener) {
        threadListeners.remove(listener);
    }

    @Override
    public final void run() {
        try {
            doRun();
            notifyListeners(true);
        } catch (Exception e) {
            notifyListeners(false);
        }
    }

    public abstract void doRun();
}

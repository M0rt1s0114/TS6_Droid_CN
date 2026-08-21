package dev.tslib;

/**
 * A TeamSpeak 3 client backed by the tscore Rust engine.
 *
 * <p>This class mirrors {@link Client}'s API so the Kotlin bridge can switch
 * backends without UI changes. Unlike {@link Client} it takes portable
 * identity text instead of an {@link Identity} native pointer.</p>
 */
public class TscoreClient implements AutoCloseable {
    private long nativePtr;

    public TscoreClient(String address, String identityExport, String nickname) {
        this(address, identityExport, nickname, null, null);
    }

    public TscoreClient(String address, String identityExport, String nickname,
                        String password, String channel) {
        this.nativePtr = nativeCreate(address, identityExport, nickname, password, channel);
        if (this.nativePtr == 0) {
            throw new TsLibException("Failed to connect to " + address);
        }
    }

    public void waitConnected() {
        checkNotClosed();
        nativeWaitConnected(nativePtr);
    }

    public Event[] processEvents() {
        checkNotClosed();
        return nativeProcessEvents(nativePtr);
    }

    public void disconnect() {
        checkNotClosed();
        nativeDisconnect(nativePtr);
    }

    public boolean isConnected() {
        checkNotClosed();
        return nativeIsConnected(nativePtr);
    }

    public int getState() {
        checkNotClosed();
        return nativeGetState(nativePtr);
    }

    public Integer getClientId() {
        checkNotClosed();
        return nativeGetClientId(nativePtr);
    }

    public Long getChannelId() {
        checkNotClosed();
        return nativeGetChannelId(nativePtr);
    }

    public Channel[] getChannels() {
        checkNotClosed();
        return nativeGetChannels(nativePtr);
    }

    public User[] getUsers() {
        checkNotClosed();
        return nativeGetUsers(nativePtr);
    }

    public Channel getChannel(long id) {
        checkNotClosed();
        return nativeGetChannel(nativePtr, id);
    }

    public User getUser(int id) {
        checkNotClosed();
        return nativeGetUser(nativePtr, id);
    }

    public ServerInfo getServerInfo() {
        checkNotClosed();
        return nativeGetServerInfo(nativePtr);
    }

    public void sendServerMessage(String msg) {
        checkNotClosed();
        nativeSendServerMessage(nativePtr, msg);
    }

    public void sendChannelMessage(String msg) {
        checkNotClosed();
        nativeSendChannelMessage(nativePtr, msg);
    }

    public void sendPrivateMessage(int userId, String msg) {
        checkNotClosed();
        nativeSendPrivateMessage(nativePtr, userId, msg);
    }

    public void moveToChannel(long channelId) {
        checkNotClosed();
        nativeMoveToChannel(nativePtr, channelId);
    }

    public void syncState() {
        checkNotClosed();
        nativeSyncState(nativePtr);
    }

    public void setInputMuted(boolean muted) {
        checkNotClosed();
        nativeSetInputMuted(nativePtr, muted);
    }

    public void sendAudio(byte[] data, int codec) {
        checkNotClosed();
        nativeSendAudio(nativePtr, data, codec);
    }

    public void downloadFile(long channelId, String path) {
        checkNotClosed();
        nativeDownloadFile(nativePtr, channelId, path);
    }

    public void uploadFile(long channelId, String path, byte[] data, boolean overwrite) {
        checkNotClosed();
        nativeUploadFile(nativePtr, channelId, path, data, overwrite);
    }

    public void listFiles(long channelId, String path) {
        checkNotClosed();
        nativeListFiles(nativePtr, channelId, path);
    }

    public void queryChannelPermissions(long channelId) {
        checkNotClosed();
        nativeQueryChannelPermissions(nativePtr, channelId);
    }

    public void deleteFile(long channelId, String name) {
        checkNotClosed();
        nativeDeleteFile(nativePtr, channelId, name);
    }

    public void renameFile(long channelId, String oldName, String newName) {
        checkNotClosed();
        nativeRenameFile(nativePtr, channelId, oldName, newName);
    }

    public void createDirectory(long channelId, String dirname) {
        checkNotClosed();
        nativeCreateDirectory(nativePtr, channelId, dirname);
    }

    @Override
    public void close() {
        if (nativePtr != 0) {
            nativeDestroy(nativePtr);
            nativePtr = 0;
        }
    }

    private void checkNotClosed() {
        if (nativePtr == 0) {
            throw new IllegalStateException("TscoreClient has been closed");
        }
    }

    private static native long nativeCreate(String address, String identityExport,
                                            String nickname, String password,
                                            String channel);
    private static native void nativeDestroy(long ptr);
    private static native void nativeWaitConnected(long ptr);
    private static native Event[] nativeProcessEvents(long ptr);
    private static native void nativeDisconnect(long ptr);
    private static native boolean nativeIsConnected(long ptr);
    private static native int nativeGetState(long ptr);
    private static native Integer nativeGetClientId(long ptr);
    private static native Long nativeGetChannelId(long ptr);
    private static native Channel[] nativeGetChannels(long ptr);
    private static native User[] nativeGetUsers(long ptr);
    private static native Channel nativeGetChannel(long ptr, long id);
    private static native User nativeGetUser(long ptr, int id);
    private static native ServerInfo nativeGetServerInfo(long ptr);
    private static native void nativeSendServerMessage(long ptr, String msg);
    private static native void nativeSendChannelMessage(long ptr, String msg);
    private static native void nativeSendPrivateMessage(long ptr, int userId, String msg);
    private static native void nativeMoveToChannel(long ptr, long channelId);
    private static native void nativeSyncState(long ptr);
    private static native void nativeSetInputMuted(long ptr, boolean muted);
    private static native void nativeSendAudio(long ptr, byte[] data, int codec);
    private static native void nativeDownloadFile(long ptr, long channelId, String path);
    private static native void nativeUploadFile(long ptr, long channelId, String path, byte[] data, boolean overwrite);
    private static native void nativeListFiles(long ptr, long channelId, String path);
    private static native void nativeQueryChannelPermissions(long ptr, long channelId);
    private static native void nativeDeleteFile(long ptr, long channelId, String name);
    private static native void nativeRenameFile(long ptr, long channelId, String oldName, String newName);
    private static native void nativeCreateDirectory(long ptr, long channelId, String dirname);
}

package chatup.tcp;

import chatup.model.Message;
import chatup.model.MessageCache;

public class SyncRoom {

    public int roomId;
    public int length;

    public SyncRoom()
    {
        messageCache = new MessageCache(100);
    }

    public SyncRoom(int paramId, int paramLength) {
        roomId = paramId;
        length = paramLength;
        messageCache = new MessageCache(length);
    }

    public SyncRoom(int paramId, final MessageCache paramCache) {
        roomId = paramId;
        length = paramCache.size();
        messageCache = paramCache;
    }

    public void insert(int paramId, final Message paramMessage) {
        messageCache.add(paramId, paramMessage);
    }

    public MessageCache messageCache;
}
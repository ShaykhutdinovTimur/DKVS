package dkvs.messages.leaderAdressed;

import dkvs.messages.Message;

public abstract class LeaderMessage extends Message {
    public LeaderMessage(int fromId) {
        this.fromId = fromId;
    }
}

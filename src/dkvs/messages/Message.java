package dkvs.messages;

import dkvs.Ballot;
import dkvs.messages.acceptorAdressed.PhaseOneRequest;
import dkvs.messages.acceptorAdressed.PhaseTwoRequest;
import dkvs.messages.leaderAdressed.PhaseOneResponse;
import dkvs.messages.leaderAdressed.PhaseTwoResponse;
import dkvs.messages.leaderAdressed.ProposeMessage;
import dkvs.messages.replicaAdressed.Decision;
import dkvs.messages.replicaAdressed.DeleteRequest;
import dkvs.messages.replicaAdressed.GetRequest;
import dkvs.messages.replicaAdressed.SetRequest;

import java.util.Arrays;

public abstract class Message {
    protected int fromId;

    public static Message parse(int fromId, String[] parts) {
        switch (parts[0]) {
            case "node":
                return new NodeMessage(Integer.parseInt(parts[1]));
            case "ping":
                return new Ping(fromId);
            case "pong":
                return new Pong(fromId);
            case "decision":
                return new Decision(Integer.parseInt(parts[1]),
                        Command.parse(Arrays.copyOfRange(parts, 2, parts.length)));
            case "propose":
                return new ProposeMessage(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                        Command.parse(Arrays.copyOfRange(parts, 3, parts.length)));
            case "p1a":
                return new PhaseOneRequest(Integer.parseInt(parts[1]), Ballot.parse(parts[2]));
            case "p2a":
                return new PhaseTwoRequest(Integer.parseInt(parts[1]),
                        Proposal.parse(Arrays.copyOfRange(parts, 2, parts.length)));
            case "p1b":
                return PhaseOneResponse.parse(parts);
            case "p2b":
                return new PhaseTwoResponse(Integer.parseInt(parts[1]), Ballot.parse(parts[2]),
                        Proposal.parse(Arrays.copyOfRange(parts, 3, parts.length)));
            case "get":
                return new GetRequest(fromId, parts[1]);
            case "set":
                return new SetRequest(fromId, parts[1], parts[2]);
            case "delete":
                return new DeleteRequest(fromId, parts[1]);
            case "disconnect":
                return new DisconnectMessage(Integer.parseInt(parts[1]));
            default:
                throw new IllegalArgumentException("Unknown message." + parts[0]);
        }
    }

    public int getSource() {
        return fromId;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
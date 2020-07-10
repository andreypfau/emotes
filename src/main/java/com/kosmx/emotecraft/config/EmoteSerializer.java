package com.kosmx.emotecraft.config;

import com.google.gson.*;
import com.kosmx.emotecraft.Emote;

import java.lang.reflect.Type;



public class EmoteSerializer implements JsonDeserializer<EmoteHolder> {
    public static Gson deserializer;

    public static void initilaizeDeserializer(){
        GsonBuilder builder = new GsonBuilder();

        JsonDeserializer<EmoteHolder> method = new EmoteSerializer();
        builder.registerTypeAdapter(EmoteHolder.class, method);

        deserializer = builder.create();
    }

    //Todo create error feedback about missing items (names)
    @Override
    public EmoteHolder deserialize(JsonElement p, Type typeOf, JsonDeserializationContext ctxt) throws JsonParseException {
        JsonObject node = p.getAsJsonObject();
        String author = null;
        String name = node.get("name").getAsString();
        if(node.has("author")){
            author = node.get("author").getAsString();
        }
        String description = null;
        if(node.has("description")){
            description = node.get("description").getAsString();
        }
        Emote emote = emoteDeserializer(node.getAsJsonObject("emote"));
        return new EmoteHolder(emote, name, description, author, node.hashCode());
    }

    private Emote emoteDeserializer(JsonObject node){
        int beginTick = 0;
        if(node.has("beginTick")){
            beginTick = node.get("beginTick").getAsInt();
        }
        int endTick = node.get("endTick").getAsInt();
        int resetTick = node.has("stopTick") ? node.get("stopTick").getAsInt() : endTick;
        boolean degrees = !node.has("degrees") || node.get("degrees").getAsBoolean();
        Emote emote = new Emote(beginTick, endTick, resetTick);
        moveDeserializer(emote, node.getAsJsonArray("moves"), degrees);
        return emote;
    }

    private void moveDeserializer(Emote emote, JsonArray node, boolean degrees){
        for (JsonElement n : node) {
            JsonObject obj = n.getAsJsonObject();
            int tick = obj.get("tick").getAsInt();
            String easing = obj.has("easing") ? obj.get("easing").getAsString() : "linear";
            int turn = obj.has("turn") ? obj.get("turn").getAsInt() : 0;
            addBodyPartIfExists(emote, emote.head, "head", obj, degrees, tick, easing, turn);
            addBodyPartIfExists(emote, emote.torso, "torso", obj, degrees, tick, easing, turn);
            addBodyPartIfExists(emote, emote.rightArm, "rightArm", obj, degrees, tick, easing, turn);
            addBodyPartIfExists(emote, emote.leftArm, "leftArm", obj, degrees, tick, easing, turn);
            addBodyPartIfExists(emote, emote.rightLeg, "rightLeg", obj, degrees, tick, easing, turn);
            addBodyPartIfExists(emote, emote.leftLeg, "leftLeg", obj, degrees, tick, easing, turn);
        }
    }
    private void addBodyPartIfExists(Emote emote, Emote.BodyPart part, String name, JsonObject node, boolean degrees, int tick, String easing, int turn){
        if(node.has(name)){
            JsonObject partNode = node.get(name).getAsJsonObject();
            addPartIfExists(emote, part.x, "x", partNode, degrees, tick, easing, turn);
            addPartIfExists(emote, part.y, "y", partNode, degrees, tick, easing, turn);
            addPartIfExists(emote, part.z, "z", partNode, degrees, tick, easing, turn);
            addPartIfExists(emote, part.pitch, "pitch", partNode, degrees, tick, easing, turn);
            addPartIfExists(emote, part.yaw, "yaw", partNode, degrees, tick, easing, turn);
            addPartIfExists(emote, part.roll, "roll", partNode, degrees, tick, easing, turn);
        }
    }
    private void addPartIfExists(Emote emote, Emote.Part part, String name, JsonObject node, boolean degrees, int tick, String easing, int turn){
        if(node.has(name)){
            emote.addMove(part, tick, node.get(name).getAsFloat(), easing, turn, degrees);
        }
    }
}

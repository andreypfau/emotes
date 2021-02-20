package com.kosmx.emotes.main;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.kosmx.emotes.Client;
import com.kosmx.emotes.Main;
import com.kosmx.emotes.common.SerializableConfig;
import com.kosmx.emotes.common.emote.EmoteData;
import com.kosmx.emotes.common.tools.Pair;
import com.kosmx.emotes.executor.EmoteInstance;
import com.kosmx.emotes.executor.dataTypes.IIdentifier;
import com.kosmx.emotes.executor.dataTypes.InputKey;
import com.kosmx.emotes.executor.dataTypes.Text;
import com.kosmx.emotes.main.config.ClientConfig;
import com.kosmx.emotes.mixinInterface.EmotePlayerInterface;
import com.kosmx.emotes.model.EmotePlayer;
import com.kosmx.emotes.network.ClientNetwork;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import com.kosmx.emotes.executor.dataTypes.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class EmoteHolder {
    public final EmoteData emote;
    public final Text name;
    public final Text description;
    public final Text author;
    public final int hash; // The emote's identifier hash
    public static List<EmoteHolder> list = new ArrayList<>(); // static array of all imported emotes
    public InputKey keyBinding = EmoteInstance.instance.getDefaults().getUnknownKey(); // assigned keybinding
    @Nullable
    public NativeImageBackedTexture nativeIcon = null;
    @Nullable
    private IIdentifier iconIdentifier = null;
    @Nullable
    public Object iconName = null; //Icon name

    public boolean isFromGeckoLib = false;

    /**
     * was it imported by {@link com.kosmx.emotes.main.quarktool.QuarkReader}
     */
    public boolean isQuarkEmote = false;

    public EmoteHolder setQuarkEmote(boolean bl){
        this.isQuarkEmote = bl;
        return this;
    }

    /**
     * @param emote       {@link EmoteData}
     * @param name        Emote name
     * @param description Emote decription
     * @param author      Name of the Author
     */
    public EmoteHolder(EmoteData emote, Text name, Text description, Text author, int hash){
        this.emote = emote;
        this.name = name;
        this.author = author;
        this.description = description;
        this.hash = hash;
    }

    /**
     * Bind keys to emotes from config class
     * @param config
     */
    public static void bindKeys(ClientConfig config){
        config.emotesWithKey = new ArrayList<>();
        config.emotesWithHash = new ArrayList<>();
        for(EmoteHolder emote : list){
            if(! emote.keyBinding.equals(EmoteInstance.instance.getDefaults().getUnknownKey())){
                config.emotesWithKey.add(emote);
                config.emotesWithHash.add(new Pair<>(emote.hash, emote.keyBinding.getTranslationKey()));
            }
        }
        config.fastMenuHash = new int[8];
        for(int i = 0; i != 8; i++){
            if(config.fastMenuEmotes[i] != null){
                config.fastMenuHash[i] = config.fastMenuEmotes[i].hash;
            }
        }
    }


    /**
     * just clear the {@link EmoteHolder#list} before reimporting emotes
     */
    public static void clearEmotes(){
        for(EmoteHolder emoteHolder : list){
            if(emoteHolder.iconIdentifier != null){
                MinecraftClient.getInstance().getTextureManager().destroyTexture(emoteHolder.iconIdentifier);
                assert emoteHolder.nativeIcon != null;
                emoteHolder.nativeIcon.close();
            }
        }
        list = new ArrayList<>();
    }

    /**
     *
     * @param path try to import emote icon
     */
    public void bindIcon(Object path){
        if(path instanceof String || path instanceof File) this.iconName = path;
        else Main.log(Level.FATAL, "Can't use " + path.getClass() + " as file");
    }

    public void assignIcon(File file){
        if(file.isFile()){
            try{
                assignIcon(new FileInputStream(file));
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }
        }
    }

    public void assignIcon(String str){
        assignIcon(Client.class.getResourceAsStream(str));
    }

    public Identifier getIconIdentifier(){
        if(iconIdentifier == null && this.iconName != null){
            if(this.iconName instanceof String) assignIcon((String) this.iconName);
            else if(this.iconName instanceof File) assignIcon((File) this.iconName);
        }
        return iconIdentifier;
    }

    public void assignIcon(InputStream inputStream){
        try{
            Throwable throwable = null;

            try{
                NativeImage image = NativeImage.read(inputStream);
                NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(image);
                this.iconIdentifier = new Identifier(Main.MOD_ID, "icon" + this.hash);
                MinecraftClient.getInstance().getTextureManager().registerTexture(this.iconIdentifier, nativeImageBackedTexture);
                this.nativeIcon = nativeImageBackedTexture;
            }catch(IOException e){
                throwable = e;
                throw e;
            }finally{
                try{
                    inputStream.close();
                }catch(Throwable throwable1){
                    if(throwable != null) throwable.addSuppressed(throwable1);
                }
            }
        }catch(Throwable var){
            Main.log(Level.ERROR, "Can't open emote icon: " + var);
            this.iconIdentifier = null;
            this.nativeIcon = null;
        }
    }


    //public void setKeyBinding(InputUtil.Key key, )

    /**
     * @return Playable EmotePlayer
     */
    public EmoteData getEmote(){
        return emote;
    }

    public static EmoteHolder getEmoteFromHash(int hash){
        for(EmoteHolder emote : list){
            if(emote.hash == hash){
                return emote;
            }
        }
        return null;
    }

    public static List<EmoteHolder> deserializeJson(BufferedReader json) throws JsonParseException{     //throws BowlingBall XD
        return Serializer.serializer.fromJson(json, new TypeToken<List<EmoteHolder>>(){}.getType());
    }

    public static void addEmoteToList(BufferedReader json) throws JsonParseException{
        list.addAll(deserializeJson(json));
    }

    public static void addEmoteToList(EmoteHolder hold){
        list.add(hold);
    }
    public static void addEmoteToList(List<EmoteHolder> hold){
        list.addAll(hold);
    }

    public static boolean playEmote(EmoteData emote, PlayerEntity player){
        return playEmote(emote, player, null);
    }

    public static boolean playEmote(EmoteData emote, PlayerEntity player, @Nullable EmoteHolder emoteHolder){
        if(canPlayEmote(player)){
            return ClientNetwork.clientStartEmote(emote, player, emoteHolder);
        }else{
            return false;
        }
    }

    private static boolean canPlayEmote(PlayerEntity entity){
        if(! canRunEmote(entity)) return false;
        if(entity != MinecraftClient.getInstance().getCameraEntity()) return false;
        EmotePlayerInterface target = (EmotePlayerInterface) entity;
        return ! (EmotePlayer.isRunningEmote(target.getEmote()) && ! target.getEmote().isLoopStarted());
    }

    /**
     * Check if the emote can be played.
     * @param entity Witch entity (player)
     * @return True if possible to play
     */
    public static boolean canRunEmote(Entity entity){
        if(! (entity instanceof AbstractClientPlayerEntity)) return false;
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
        if(player.getPose() != EntityPose.STANDING) return false;
        //System.out.println(player.getPos().distanceTo(new Vec3d(player.prevX, player.prevY, player.prevZ)));
        return ! (player.getPos().distanceTo(new Vec3d(player.prevX, MathHelper.lerp(Main.config.yRatio, player.prevY, player.getPos().getY()), player.prevZ)) > Main.config.stopThreshold);
    }

    public boolean playEmote(PlayerEntity playerEntity){
        return playEmote(this.emote, playerEntity, this);
    }
}


/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.lc;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;

import com.github.ucchyocean.lc.event.LunaChatChannelChatEvent;
import com.github.ucchyocean.lc.event.LunaChatChannelMemberChangedEvent;
import com.github.ucchyocean.lc.event.LunaChatChannelMessageEvent;
import com.github.ucchyocean.lc.japanize.JapanizeType;

/**
 * チャンネル
 * @author ucchy
 */
@SerializableAs("Channel")
public class Channel implements ConfigurationSerializable {

    private static final String FOLDER_NAME_CHANNELS = "channels";

    private static final String INFO_FIRSTLINE = Resources.get("channelInfoFirstLine");
    private static final String INFO_PREFIX = Resources.get("channelInfoPrefix");
    private static final String INFO_GLOBAL = Resources.get("channelInfoGlobal");
    private static final String INFO_BROADCAST = Resources.get("channelInfoBroadcast");
    private static final String INFO_SECRET = Resources.get("channelInfoSecret");
    private static final String INFO_PASSWORD = Resources.get("channelInfoPassword");
    private static final String INFO_WORLDCHAT = Resources.get("channelInfoWorldChat");
    private static final String INFO_RANGECHAT = Resources.get("channelInfoRangeChat");
    private static final String INFO_FORMAT = Resources.get("channelInfoFormat");
    private static final String INFO_BANNED = Resources.get("channelInfoBanned");
    private static final String INFO_MUTED = Resources.get("channelInfoMuted");

    private static final String LIST_ENDLINE = Resources.get("listEndLine");
    private static final String LIST_FORMAT = Resources.get("listFormat");

    private static final String DEFAULT_FORMAT = Resources.get("defaultFormat");
    private static final String DEFAULT_FORMAT_FOR_PERSONAL =
            Resources.get("defaultFormatForPersonalChat");
    private static final String MSG_JOIN = Resources.get("joinMessage");
    private static final String MSG_QUIT = Resources.get("quitMessage");
    private static final String MSG_BAN_EXPIRED = Resources.get("expiredBan");
    private static final String MSG_MUTE_EXPIRED = Resources.get("expiredMute");
    private static final String MSG_BAN_EXPIRED_PLAYER = Resources.get("cmdmsgPardoned");
    private static final String MSG_MUTE_EXPIRED_PLAYER = Resources.get("cmdmsgUnmuted");

    private static final String PREINFO = Resources.get("infoPrefix");
    private static final String PREERR = Resources.get("errorPrefix");

    private static final String NGWORD_PREFIX = Resources.get("ngwordPrefix");
    private static final String MSG_KICKED = Resources.get("cmdmsgKicked");
    private static final String MSG_BANNED = Resources.get("cmdmsgBanned");
    private static final String MSG_MUTED = Resources.get("cmdmsgMuted");

    private static final String MSG_NO_RECIPIENT = Resources.get("noRecipientMessage");

    private static final String ERRMSG_MUTED = Resources.get("errmsgMuted");

    private static final String KEY_NAME = "name";
    private static final String KEY_DESC = "desc";
    private static final String KEY_FORMAT = "format";
    private static final String KEY_MEMBERS = "members";
    private static final String KEY_BANNED = "banned";
    private static final String KEY_MUTED = "muted";
    private static final String KEY_HIDED = "hided";
    private static final String KEY_MODERATOR = "moderator";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_VISIBLE = "visible";
    private static final String KEY_COLOR = "color";
    private static final String KEY_BROADCAST = "broadcast";
    private static final String KEY_WORLD = "world";
    private static final String KEY_RANGE = "range";
    private static final String KEY_BAN_EXPIRES = "ban_expires";
    private static final String KEY_MUTE_EXPIRES = "mute_expires";

    /** 参加者 */
    private List<String> members;

    /** チャンネルモデレータ */
    private List<String> moderator;

    /** BANされたプレイヤー */
    private List<String> banned;

    /** Muteされたプレイヤー */
    private List<String> muted;

    /** Hideしているプレイヤー */
    private List<String> hided;

    /** チャンネルの名称 */
    private String name;

    /** チャンネルの説明文 */
    private String description;

    /** チャンネルのパスワード */
    private String password;

    /** チャンネルリストに表示されるかどうか */
    private boolean visible;

    /** チャンネルのカラー */
    private String colorCode;

    /** メッセージフォーマット<br>
     * 指定可能なキーワードは下記のとおり<br>
     * %ch - チャンネル名<br>
     * %username - ユーザー名<br>
     * %msg - メッセージ<br>
     * %prefix - PermissionsExに設定するprefix<br>
     * %suffix - PermissionsExに設定するsuffix<br>
     * %color - チャンネルのカラーコード
     * */
    private String format;

    /** ブロードキャストチャンネルかどうか */
    private boolean broadcastChannel;

    /** ワールドチャットかどうか */
    private boolean isWorldRange;

    /** チャットの可聴範囲 0は無制限 */
    private int chatRange;

    /** 期限付きBANの期限（key=プレイヤー名、value=期日（ミリ秒）） */
    private Map<String, Long> banExpires;

    /** 期限付きMuteの期限（key=プレイヤー名、value=期日（ミリ秒）） */
    private Map<String, Long> muteExpires;

    /** 1:1チャットの相手名 */
    private String privateMessageTo;

    /** ロガー */
    private LunaChatLogger logger;

    /**
     * コンストラクタ
     * @param name チャンネルの名称
     */
    protected Channel(String name) {
        this.name = name;
        this.description = "";
        this.members = new ArrayList<String>();
        this.banned = new ArrayList<String>();
        this.muted = new ArrayList<String>();
        this.hided = new ArrayList<String>();
        this.moderator = new ArrayList<String>();
        this.password = "";
        this.visible = true;
        this.colorCode = "";
        this.broadcastChannel = false;
        this.isWorldRange = false;
        this.chatRange = 0;
        this.banExpires = new HashMap<String, Long>();
        this.muteExpires = new HashMap<String, Long>();
        this.privateMessageTo = "";

        if ( isPersonalChat() ) {
            this.format = DEFAULT_FORMAT_FOR_PERSONAL;
        } else {
            this.format = DEFAULT_FORMAT;
            logger = new LunaChatLogger(name);
        }
    }

    /**
     * @return 1:1チャットかどうか
     */
    public boolean isPersonalChat() {
        return name.contains(">");
    }

    /**
     * @return ブロードキャストチャンネルかどうか
     */
    public boolean isBroadcastChannel() {
        return (isGlobalChannel() || broadcastChannel);
    }

    /**
     * @return グローバルチャンネルかどうか
     */
    public boolean isGlobalChannel() {
        return name.equals(LunaChat.config.getGlobalChannel());
    }

    /**
     * @return 強制参加チャンネルかどうか
     */
    public boolean isForceJoinChannel() {
        return LunaChat.config.getForceJoinChannels().contains(name);
    }

    /**
     * このチャンネルのモデレータ権限を持っているかどうかを確認する
     * @param sender 権限を確認する対象
     * @return チャンネルのモデレータ権限を持っているかどうか
     */
    public boolean hasModeratorPermission(CommandSender sender) {
        return sender.isOp() || moderator.contains(sender.getName()) ||
                sender.hasPermission("lunachat-admin.mod-all-channels");
    }

    /**
     * このチャットに発言をする
     * @param player 発言をするプレイヤー
     * @param message 発言をするメッセージ
     */
    public void chat(Player player, String message) {

        // Muteされているかどうかを確認する
        if ( player != null && muted.contains(player.getName()) ) {
            player.sendMessage( PREERR + ERRMSG_MUTED );
            return;
        }

        String preReplaceMessage = message;

        // 一時的にJapanizeスキップ設定かどうかを確認する
        boolean skipJapanize = false;
        String marker = LunaChat.config.getNoneJapanizeMarker();
        if ( !marker.equals("") && message.startsWith(marker) ) {
            skipJapanize = true;
            message = message.substring(marker.length());
        }

        // NGワード発言をしたかどうかのチェックとマスク
        boolean isNG = false;
        String maskedMessage = message;
        for ( String word : LunaChat.config.getNgword() ) {
            if ( maskedMessage.contains(word) ) {
                maskedMessage = maskedMessage.replace(
                        word, Utility.getAstariskString(word.length()));
                isNG = true;
            }
        }

        // キーワード置き換え
        String msgFormat = replaceKeywords(format, player);

        // カラーコード置き換え
        maskedMessage = Utility.replaceColorCode(maskedMessage);

        // イベントコール
        LunaChatChannelChatEvent event =
                new LunaChatChannelChatEvent(this.name, player,
                        preReplaceMessage, maskedMessage, msgFormat);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if ( event.isCancelled() ) {
            return;
        }
        msgFormat = event.getMessageFormat();
        maskedMessage = event.getNgMaskedMessage();

        // 2byteコードを含むか、半角カタカナのみなら、Japanize変換は行わない
        if ( !skipJapanize &&
                ( message.getBytes().length > message.length() ||
                  message.matches("[ \\uFF61-\\uFF9F]+") ) ) {
            skipJapanize = true;
        }

        // Japanize変換タスクを作成する
        boolean isIncludeSyncChat = true;
        DelayedJapanizeConvertTask delayedTask = null;
        if ( !skipJapanize &&
                LunaChat.manager.isPlayerJapanize(player.getName()) &&
                LunaChat.config.getJapanizeType() != JapanizeType.NONE ) {

            int lineType = LunaChat.config.getJapanizeDisplayLine();
            String jpFormat;
            String messageFormat = null;
            if ( lineType == 1 ) {
                jpFormat = LunaChat.config.getJapanizeLine1Format();
                messageFormat = msgFormat;
                isIncludeSyncChat = false;
            } else {
                jpFormat = LunaChat.config.getJapanizeLine2Format();
            }

            // タスクを作成しておく
            delayedTask = new DelayedJapanizeConvertTask(maskedMessage,
                            LunaChat.config.getJapanizeType(),
                            this, player, jpFormat, messageFormat);
        }

        if ( isIncludeSyncChat ) {
            // メッセージの送信
            sendMessage(player, maskedMessage, msgFormat, true);
        }

        // 非同期実行タスクがある場合、追加で実行する
        if ( delayedTask != null ) {
            Bukkit.getScheduler().runTask(LunaChat.instance, delayedTask);
        }

        // NGワード発言者に、NGワードアクションを実行する
        if ( isNG && player != null ) {
            if ( LunaChat.config.getNgwordAction() == NGWordAction.BAN ) {
                // BANする

                if ( !isGlobalChannel() ) {
                    banned.add(player.getName());
                    removeMember(player.getName());
                    String temp = PREINFO + NGWORD_PREFIX + MSG_BANNED;
                    String m = String.format(temp, name);
                    player.sendMessage(m);
                }

            } else if ( LunaChat.config.getNgwordAction() == NGWordAction.KICK ) {
                // キックする

                if ( !isGlobalChannel() ) {
                    removeMember(player.getName());
                    String temp = PREINFO + NGWORD_PREFIX + MSG_KICKED;
                    String m = String.format(temp, name);
                    player.sendMessage(m);
                }

            } else if ( LunaChat.config.getNgwordAction() == NGWordAction.MUTE ) {
                // Muteする

                muted.add(player.getName());
                save();
                String temp = PREINFO + NGWORD_PREFIX + MSG_MUTED;
                String m = String.format(temp, name);
                player.sendMessage(m);
            }
        }
    }

    /**
     * ほかの連携先などから、このチャットに発言する
     * @param player プレイヤー名
     * @param source 連携元を判別する文字列
     * @param message メッセージ
     */
    public void chatFromOtherSource(String player, String source, String message) {

        // 表示名
        String name = player + "@" + source;

        // NGワード発言のマスク
        String maskedMessage = message;
        for ( String word : LunaChat.config.getNgword() ) {
            if ( maskedMessage.contains(word) ) {
                maskedMessage = maskedMessage.replace(
                        word, Utility.getAstariskString(word.length()));
            }
        }

        // キーワード置き換え
        String msgFormat = replaceKeywords(format, null);
        msgFormat = msgFormat.replace("%username", name);
        msgFormat = msgFormat.replace("%prefix", "");
        msgFormat = msgFormat.replace("%suffix", "");

        // カラーコード置き換え
        maskedMessage = Utility.replaceColorCode(maskedMessage);

        // メッセージの送信
        sendMessage(null, maskedMessage, msgFormat, false);
    }

    /**
     * メンバーを追加する
     * @param name 追加するメンバー名
     */
    public void addMember(String name) {

        // イベントコール
        LunaChatChannelMemberChangedEvent event =
                new LunaChatChannelMemberChangedEvent(this.name, this.members);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if ( event.isCancelled() ) {
            return;
        }

        // メンバー追加
        if ( members.size() == 0 && moderator.size() == 0 ) {
            moderator.add(name);
        }
        if ( !members.contains(name) ) {
            members.add(name);
            sendJoinQuitMessage(true, name);
            save();
        }
    }

    /**
     * メンバーを削除する
     * @param name 削除するメンバー名
     */
    public void removeMember(String name) {

        // イベントコール
        LunaChatChannelMemberChangedEvent event =
                new LunaChatChannelMemberChangedEvent(this.name, this.members);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if ( event.isCancelled() ) {
            return;
        }

        // デフォルト発言先が退出するチャンネルと一致する場合、
        // デフォルト発言先を削除する
        Channel def = LunaChat.manager.getDefaultChannel(name);
        if ( def != null && def.name.equals(this.name) ) {
            LunaChat.manager.removeDefaultChannel(name);
        }

        // 実際にメンバーから削除する
        if ( members.contains(name) ) {
            members.remove(name);
            sendJoinQuitMessage(false, name);
            if ( LunaChat.config.isZeroMemberRemove() && members.size() <= 0 ) {
                LunaChat.manager.removeChannel(this.name);
                return;
            } else {
                save();
            }
        }

        // 非表示設定プレイヤーだったら、リストから削除する
        if ( hided.contains(name) ) {
            hided.remove(name);
        }

        // モデレーターだった場合は、モデレーターから除去する
        if ( moderator.contains(name) ) {
            moderator.remove(name);
        }
    }

    /**
     * 入退室メッセージを流す
     * @param isJoin 入室かどうか（falseなら退室）
     * @param player 入退室したプレイヤー名
     */
    private void sendJoinQuitMessage(boolean isJoin, String player) {

        // 1:1チャットなら、入退室メッセージは表示しない
        if ( isPersonalChat() ) {
            return;
        }

        String msg;
        if ( isJoin ) {
            msg = MSG_JOIN;
        } else {
            msg = MSG_QUIT;
        }

        // キーワード置き換え
        msg = replaceKeywordsForSystemMessages(msg, player);

        sendMessage(null, msg, null, false);
    }

    /**
     * メッセージを表示します。指定したプレイヤーの発言として処理されます。
     * @param player プレイヤー（ワールドチャット、範囲チャットの場合は必須です）
     * @param message メッセージ
     * @param format フォーマット
     * @param sendDynmap dynmapへ送信するかどうか
     */
    protected void sendMessage(Player player, String message,
            String format, boolean sendDynmap) {

        String originalMessage = new String(message);

        // 受信者を設定する
        ArrayList<Player> recipients = new ArrayList<Player>();
        boolean isRangeChat = false;

        if ( isBroadcastChannel() ) {
            // ブロードキャストチャンネル

            if ( isWorldRange && player != null ) {
                isRangeChat = true;
                World w = player.getWorld();

                if ( chatRange > 0 ) {
                    // 範囲チャット

                    for ( Player p : Bukkit.getOnlinePlayers() ) {
                        if ( p.getWorld().equals(w) &&
                                player.getLocation().distance(p.getLocation()) <= chatRange &&
                                !hided.contains(p.getName()) ) {
                            recipients.add(p);
                        }
                    }

                } else {
                    // ワールドチャット

                    for ( Player p : Bukkit.getOnlinePlayers() ) {
                        if ( p.getWorld().equals(w) && !hided.contains(p.getName()) ) {
                            recipients.add(p);
                        }
                    }
                }

            } else {
                // 通常ブロードキャスト（全員へ送信）

                for ( Player p : Bukkit.getOnlinePlayers() ) {
                    if ( !hided.contains(p.getName()) ) {
                        recipients.add(p);
                    }
                }
            }

        } else {
            // 通常チャンネル

            for ( String name : members ) {
                Player p = Utility.getPlayerExact(name);
                if ( p != null && !hided.contains(p.getName()) ) {
                    recipients.add(p);
                }
            }
        }

        // フォーマットがある場合は置き換える
        if ( format != null ) {
            message = format.replace("%msg", message);
        }

        // イベントコール
        LunaChatChannelMessageEvent event =
                new LunaChatChannelMessageEvent(
                        name, player, message, recipients);
        Bukkit.getPluginManager().callEvent(event);
        message = event.getMessage();
        recipients = event.getRecipients();

        // 通常ブロードキャストなら、設定に応じてdynmapへ送信する
        if ( LunaChat.config.isSendBroadcastChannelChatToDynmap() &&
                sendDynmap &&
                LunaChat.dynmap != null &&
                isBroadcastChannel() &&
                !isWorldRange ) {
            if ( LunaChat.config.isSendFormattedMessageToDynmap() ) {
                if ( player != null ) {
                    LunaChat.dynmap.chat(player, message);
                } else {
                    LunaChat.dynmap.broadcast(message);
                }
            } else {
                if ( player != null ) {
                    LunaChat.dynmap.chat(player, originalMessage);
                } else {
                    LunaChat.dynmap.broadcast(originalMessage);
                }
            }
        }

        // 送信する
        for ( Player p : recipients ) {
            p.sendMessage(message);
        }

        // 受信者が自分以外いない場合は、メッセージを表示する
        if ( isRangeChat && (
                recipients.size() == 0 ||
                (recipients.size() == 1 &&
                 recipients.get(0).getName().equals(player.getName()) ) ) ) {
            String msg = replaceKeywordsForSystemMessages(MSG_NO_RECIPIENT, "");
            player.sendMessage(msg);
        }

        // ロギング
        log(message);
    }

    /**
     * チャンネル情報を返す
     * @param forModerator モデレータ向けの情報を含めるかどうか
     * @return チャンネル情報
     */
    public ArrayList<String> getInfo(boolean forModerator) {

        ArrayList<String> info = new ArrayList<String>();
        info.add(INFO_FIRSTLINE);

        // チャンネル名、参加人数、総人数、チャンネル説明文
        info.add( String.format(
                LIST_FORMAT, name, getOnlineNum(), getTotalNum(), description) );

        // 参加メンバー一覧
        if ( isGlobalChannel() ) {
            info.add(INFO_GLOBAL);
        } else if ( isBroadcastChannel() ) {
            info.add(INFO_BROADCAST);
        } else {
            // メンバーを、5人ごとに表示する
            StringBuffer buf = new StringBuffer();
            buf.append(INFO_PREFIX);

            for ( int i=0; i<members.size(); i++ ) {

                if ( i%5 == 0 && i != 0 ) {
                    info.add(buf.toString());
                    buf = new StringBuffer();
                    buf.append(INFO_PREFIX);
                }

                String name = members.get(i);
                String disp;
                if ( moderator.contains(name) ) {
                    name = "@" + name;
                }
                if ( isOnlinePlayer(members.get(i)) ) {
                    if ( hided.contains(members.get(i)) )
                        disp = ChatColor.DARK_AQUA + name;
                    else
                        disp = ChatColor.WHITE + name;
                } else {
                    disp = ChatColor.GRAY + name;
                }
                buf.append(disp + ",");
            }

            info.add(buf.toString());
        }

        // シークレットチャンネルかどうか
        if ( !visible ) {
            info.add(INFO_SECRET);
        }

        // パスワード設定があるかどうか
        if ( password.length() > 0 ) {
            if ( !forModerator ) {
                info.add(INFO_PASSWORD);
            } else {
                info.add(INFO_PASSWORD + " " + password);
            }
        }

        // 範囲チャット、ワールドチャット
        if ( isWorldRange && chatRange > 0 ) {
            info.add(String.format(INFO_RANGECHAT, chatRange));
        } else if ( isWorldRange ) {
            info.add(INFO_WORLDCHAT);
        }

        if ( forModerator ) {

            // フォーマット情報
            info.add(INFO_FORMAT + format);

            // Muteリスト情報、5人ごとに表示する
            if ( muted.size() > 0 ) {
                info.add(INFO_MUTED);

                StringBuffer buf = new StringBuffer();
                buf.append(INFO_PREFIX + ChatColor.WHITE);
                for ( int i=0; i<muted.size(); i++ ) {
                    if ( i%5 == 0 && i != 0 ) {
                        info.add(buf.toString());
                        buf = new StringBuffer();
                        buf.append(INFO_PREFIX + ChatColor.WHITE);
                    }
                    buf.append(muted.get(i) + ",");
                }

                info.add(buf.toString());
            }

            // BANリスト情報、5人ごとに表示する
            if ( banned.size() > 0 ) {
                info.add(INFO_BANNED);

                StringBuffer buf = new StringBuffer();
                buf.append(INFO_PREFIX + ChatColor.WHITE);
                for ( int i=0; i<banned.size(); i++ ) {
                    if ( i%5 == 0 && i != 0 ) {
                        info.add(buf.toString());
                        buf = new StringBuffer();
                        buf.append(INFO_PREFIX + ChatColor.WHITE);
                    }
                    buf.append(banned.get(i) + ",");
                }

                info.add(buf.toString());
            }
        }

        info.add(LIST_ENDLINE);

        return info;
    }

    /**
     * 指定された名前のプレイヤーがオンラインかどうかを確認する
     * @param playerName プレイヤー名
     * @return オンラインかどうか
     */
    private boolean isOnlinePlayer(String playerName) {
        Player p = Utility.getPlayerExact(playerName);
        return ( p != null && p.isOnline() );
    }

    /**
     * チャットフォーマット内のキーワードを置き換えする
     * @param format チャットフォーマット
     * @param player プレイヤー
     * @return 置き換え結果
     */
    private String replaceKeywords(String format, Player player) {

        String msg = format;

        // テンプレートのキーワードを、まず最初に置き換える
        for ( int i=0; i<=9; i++ ) {
            String key = "%" + i;
            if ( msg.contains(key) ) {
                if ( LunaChat.manager.getTemplate("" + i) != null ) {
                    msg = msg.replace(key, LunaChat.manager.getTemplate("" + i));
                    break;
                }
            }
        }

        msg = msg.replace("%ch", name);
        //msg = msg.replace("%msg", message);
        msg = msg.replace("%color", colorCode);
        msg = msg.replace("%to", privateMessageTo);

        if ( player != null ) {
            msg = msg.replace("%username", player.getDisplayName());
            msg = msg.replace("%player", player.getName());

            if ( msg.contains("%prefix") || msg.contains("%suffix") ) {
                String prefix = "";
                String suffix = "";
                if ( LunaChat.vaultchat != null ) {
                    prefix = LunaChat.vaultchat.getPlayerPrefix(player);
                    suffix = LunaChat.vaultchat.getPlayerSuffix(player);
                }
                msg = msg.replace("%prefix", prefix);
                msg = msg.replace("%suffix", suffix);
            }
        } else {
            msg = msg.replace("%username", "");
            msg = msg.replace("%player", "");
            msg = msg.replace("%prefix", "");
            msg = msg.replace("%suffix", "");
        }

        return Utility.replaceColorCode(msg);
    }

    /**
     * チャットフォーマット内のキーワードを置き換えする
     * @param format チャットフォーマット
     * @param playerName プレイヤー名
     * @return 置き換え結果
     */
    private String replaceKeywordsForSystemMessages(String format, String playerName) {

        String msg = format;
        msg = msg.replace("%ch", name);
        msg = msg.replace("%color", colorCode);
        msg = msg.replace("%username", playerName);

        return Utility.replaceColorCode(msg);
    }

    /**
     * チャンネルのオンライン人数を返す
     * @return オンライン人数
     */
    public int getOnlineNum() {

        // ブロードキャストチャンネルならサーバー接続人数を返す
        if ( isBroadcastChannel() ) {
            return Bukkit.getOnlinePlayers().length;
        }

        // メンバーの人数を数える
        int onlineNum = 0;
        for ( String pname : members ) {
            if ( isOnlinePlayer(pname) ) {
                onlineNum++;
            }
        }
        return onlineNum;
    }

    /**
     * チャンネルの総参加人数を返す
     * @return 総参加人数
     */
    public int getTotalNum() {

        // ブロードキャストチャンネルならサーバー接続人数を返す
        if ( isBroadcastChannel() ) {
            return Bukkit.getOnlinePlayers().length;
        }

        return members.size();
    }

    /**
     * 期限付きBanや期限付きMuteをチェックし、期限が切れていたら解除を行う
     */
    public void checkExpires() {

        long now = System.currentTimeMillis();

        // 期限付きBANのチェック
        for ( String name : banExpires.keySet() ) {
            if ( banExpires.get(name) <= now ) {

                // 期限マップから削除し、BANを解除
                banExpires.remove(name);
                if ( banned.contains(name) ) {
                    banned.remove(name);
                    save();

                    // メッセージ通知を流す
                    String msg = String.format(MSG_BAN_EXPIRED, this.name, name);
                    sendMessage(null, msg, null, false);

                    Player player = Utility.getPlayerExact(name);
                    if ( player != null ) {
                        msg = PREINFO + String.format(MSG_BAN_EXPIRED_PLAYER, this.name);
                        player.sendMessage(msg);
                    }
                }
            }
        }

        // 期限付きMuteのチェック
        for ( String name : muteExpires.keySet() ) {
            if ( muteExpires.get(name) <= now ) {

                // 期限マップから削除し、Muteを解除
                muteExpires.remove(name);
                if ( muted.contains(name) ) {
                    muted.remove(name);
                    save();

                    // メッセージ通知を流す
                    String msg = String.format(MSG_MUTE_EXPIRED, this.name, name);
                    sendMessage(null, msg, null, false);

                    Player player = Utility.getPlayerExact(name);
                    if ( player != null ) {
                        msg = PREINFO + String.format(MSG_MUTE_EXPIRED_PLAYER, this.name);
                        player.sendMessage(msg);
                    }
                }
            }
        }
    }

    /**
     * シリアライズ<br>
     * ConfigurationSerializable互換のための実装。
     * @see org.bukkit.configuration.serialization.ConfigurationSerializable#serialize()
     */
    @Override
    public Map<String, Object> serialize() {

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(KEY_NAME, name);
        map.put(KEY_DESC, description);
        map.put(KEY_FORMAT, format);
        map.put(KEY_MEMBERS, members);
        map.put(KEY_BANNED, banned);
        map.put(KEY_MUTED, muted);
        map.put(KEY_HIDED, hided);
        map.put(KEY_MODERATOR, moderator);
        map.put(KEY_PASSWORD, password);
        map.put(KEY_VISIBLE, visible);
        map.put(KEY_COLOR, colorCode);
        map.put(KEY_BROADCAST, broadcastChannel);
        map.put(KEY_WORLD, isWorldRange);
        map.put(KEY_RANGE, chatRange);
        map.put(KEY_BAN_EXPIRES, banExpires);
        map.put(KEY_MUTE_EXPIRES, muteExpires);
        return map;
    }

    /**
     * デシリアライズ<br>
     * ConfigurationSerializable互換のための実装。
     * @param data デシリアライズ元のMapデータ。
     * @return デシリアライズされたクラス
     */
    public static Channel deserialize(Map<String, Object> data) {

        String name = castWithDefault(data.get(KEY_NAME), (String)null);
        List<String> members = castToStringList(data.get(KEY_MEMBERS));

        Channel channel = new Channel(name);
        channel.members = members;
        channel.description = castWithDefault(data.get(KEY_DESC), "");
        channel.format =
            castWithDefault(data.get(KEY_FORMAT), DEFAULT_FORMAT);
        channel.banned = castToStringList(data.get(KEY_BANNED));
        channel.muted = castToStringList(data.get(KEY_MUTED));
        channel.hided = castToStringList(data.get(KEY_HIDED));
        channel.moderator = castToStringList(data.get(KEY_MODERATOR));
        channel.password = castWithDefault(data.get(KEY_PASSWORD), "");
        channel.visible = castWithDefault(data.get(KEY_VISIBLE), true);
        channel.colorCode = castWithDefault(data.get(KEY_COLOR), "");
        channel.broadcastChannel = castWithDefault(data.get(KEY_BROADCAST), false);
        channel.isWorldRange = castWithDefault(data.get(KEY_WORLD), false);
        channel.chatRange = castWithDefault(data.get(KEY_RANGE), 0);
        channel.banExpires = castToStringLongMap(data.get(KEY_BAN_EXPIRES));
        channel.muteExpires = castToStringLongMap(data.get(KEY_MUTE_EXPIRES));
        return channel;
    }

    /**
     * Objectを、クラスTに変換する。nullならデフォルトを返す。
     * @param obj 変換元
     * @param def nullだった場合のデフォルト
     * @return 変換後
     */
    @SuppressWarnings("unchecked")
    private static <T> T castWithDefault(Object obj, T def) {

        if ( obj == null ) {
            return def;
        }
        return (T)obj;
    }

    /**
     * Objectを、List&lt;String&gt;に変換する。nullなら空のリストを返す。
     * @param obj 変換元
     * @return 変換後
     */
    @SuppressWarnings("unchecked")
    private static List<String> castToStringList(Object obj) {

        if ( obj == null ) {
            return new ArrayList<String>();
        }
        if ( !(obj instanceof List<?>) ) {
            return new ArrayList<String>();
        }
        return (List<String>)obj;
    }

    /**
     * Objectを、Map&lt;String, Long&gt;に変換する。nullなら空のリストを返す。
     * @param obj 変換元
     * @return 変換後
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Long> castToStringLongMap(Object obj) {

        if ( obj == null ) {
            return new HashMap<String, Long>();
        }
        if ( !(obj instanceof HashMap<?, ?>) ) {
            return new HashMap<String, Long>();
        }
        return (Map<String, Long>)obj;
    }

    /**
     * チャンネルの説明文を返す
     * @return チャンネルの説明文
     */
    public String getDescription() {
        return description;
    }

    /**
     * チャンネルの説明文を設定する
     * @param description チャンネルの説明文
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * チャンネルのパスワードを返す
     * @return チャンネルのパスワード
     */
    public String getPassword() {
        return password;
    }

    /**
     * チャンネルのパスワードを設定する
     * @param password チャンネルのパスワード
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * チャンネルの可視性を返す
     * @return チャンネルの可視性
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * チャンネルの可視性を設定する
     * @param visible チャンネルの可視性
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * チャンネルのメッセージフォーマットを返す
     * @return チャンネルのメッセージフォーマット
     */
    public String getFormat() {
        return format;
    }

    /**
     * チャンネルのメッセージフォーマットを設定する
     * @param format チャンネルのメッセージフォーマット
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * チャンネルのメンバーを返す
     * @return チャンネルのメンバー
     */
    public List<String> getMembers() {

        // ブロードキャストチャンネルなら、
        // 現在サーバーに接続している全プレイヤーをメンバーとして返す
        if ( isBroadcastChannel() ) {
            Player[] players = Bukkit.getOnlinePlayers();
            List<String> mem = new ArrayList<String>();
            for ( Player p : players ) {
                mem.add(p.getName());
            }
            return mem;
        }

        return members;
    }

    /**
     * チャンネルのモデレーターを返す
     * @return チャンネルのモデレーター
     */
    public List<String> getModerator() {
        return moderator;
    }

    /**
     * チャンネルのBANリストを返す
     * @return チャンネルのBANリスト
     */
    public List<String> getBanned() {
        return banned;
    }

    /**
     * チャンネルのMuteリストを返す
     * @return チャンネルのMuteリスト
     */
    public List<String> getMuted() {
        return muted;
    }

    /**
     * 期限付きBANの期限マップを返す（key=プレイヤー名、value=期日（ミリ秒））
     * @return banExpires
     */
    public Map<String, Long> getBanExpires() {
        return banExpires;
    }

    /**
     * 期限付きMuteの期限マップを返す（key=プレイヤー名、value=期日（ミリ秒））
     * @return muteExpires
     */
    public Map<String, Long> getMuteExpires() {
        return muteExpires;
    }

    /**
     * 非表示プレイヤーの一覧を返す
     * @return チャンネルの非表示プレイヤーの一覧
     */
    public List<String> getHided() {
        return hided;
    }

    /**
     * チャンネル名を返す
     * @return チャンネル名
     */
    public String getName() {
        return name;
    }

    /**
     * チャンネルのカラーコードを返す
     * @return チャンネルのカラーコード
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * チャンネルのカラーコードを設定する
     * @param colorCode カラーコード
     */
    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    /**
     * ブロードキャストチャンネルを設定する
     * @param broadcast ブロードキャストチャンネルにするかどうか
     */
    public void setBroadcast(boolean broadcast) {
        this.broadcastChannel = broadcast;
    }

    /**
     * チャットを同ワールド内に制限するかどうかを設定する
     * @param isWorldRange 同ワールド制限するかどうか
     */
    public void setWorldRange(boolean isWorldRange) {
        this.isWorldRange = isWorldRange;
    }

    /**
     * チャットの可聴範囲を設定する
     * @param range 可聴範囲
     */
    public void setRange(int range) {
        this.chatRange = range;
    }

    /**
     * 1:1チャットのときに、会話の相手先を取得する
     * @return 会話の相手のプレイヤー名
     */
    public String getPrivateMessageTo() {
        return privateMessageTo;
    }

    /**
     * 1:1チャットのときに、会話の相手先を設定する
     * @param name 会話の相手のプレイヤー名
     */
    public void setPrivateMessageTo(String name) {
        this.privateMessageTo = name;
    }

    /**
     * チャンネルの情報をファイルに保存する。
     * @return 保存をしたかどうか。
     */
    public boolean save() {

        // フォルダーの取得と、必要に応じて作成
        File folder = new File(
                LunaChat.instance.getDataFolder(), FOLDER_NAME_CHANNELS);
        if ( !folder.exists() ) {
            folder.mkdirs();
        }

        // 1:1チャットチャンネルの場合は、何もしない。
        if ( isPersonalChat() ) {
            return false;
        }

        File file = new File(folder, name + ".yml");

        // ファイルへ保存する
        YamlConfiguration conf = new YamlConfiguration();
        Map<String, Object> data = this.serialize();
        for ( String key : data.keySet() ) {
            conf.set(key, data.get(key));
        }
        try {
            conf.save(file);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * チャンネルの情報を保存したファイルを、削除する。
     * @return 削除したかどうか。
     */
    protected boolean remove() {

        // フォルダーの取得
        File folder = new File(
                LunaChat.instance.getDataFolder(), FOLDER_NAME_CHANNELS);
        if ( !folder.exists() ) {
            return false;
        }
        File file = new File(folder, name + ".yml");
        if ( !file.exists() ) {
            return false;
        }

        // ファイルを削除
        return file.delete();
    }

    /**
     * チャンネルの情報を保存したファイルから全てのチャンネルを復元して返す。
     * @return 全てのチャンネル
     */
    protected static HashMap<String, Channel> loadAllChannels() {

        // フォルダーの取得
        File folder = new File(
                LunaChat.instance.getDataFolder(), FOLDER_NAME_CHANNELS);
        if ( !folder.exists() ) {
            return new HashMap<String, Channel>();
        }

        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        HashMap<String, Channel> result = new HashMap<String, Channel>();
        for ( File file : files ) {
            YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);
            Map<String, Object> data = new HashMap<String, Object>();
            for ( String key : config.getKeys(false) ) {
                data.put(key, config.get(key));
            }
            Channel channel = deserialize(data);
            result.put(channel.name.toLowerCase(), channel);
        }

        return result;
    }

    /**
     * ログを記録する
     * @param message 記録するメッセージ
     */
    private void log(String message) {

        if ( LunaChat.config.isDisplayChatOnConsole() ) {
            Bukkit.getLogger().info(ChatColor.stripColor(message));
        }
        if ( LunaChat.config.isLoggingChat() && logger != null ) {
            logger.log(ChatColor.stripColor(message));
        }
    }
}

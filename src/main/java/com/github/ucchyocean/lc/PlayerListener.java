/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.lc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.github.ucchyocean.lc.japanize.JapanizeType;

/**
 * プレイヤーの行動を監視するリスナ
 * @author ucchy
 */
public class PlayerListener implements Listener {

    /**
     * プレイヤーのチャットごとに呼び出されるメソッド
     * @param event チャットイベント
     */
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        // 頭にglobalMarkerが付いている場合は、グローバル発言にする
        if ( LunaChat.config.getGlobalMarker() != null &&
                !LunaChat.config.getGlobalMarker().equals("") &&
                event.getMessage().startsWith(LunaChat.config.getGlobalMarker()) &&
                event.getMessage().length() > LunaChat.config.getGlobalMarker().length() ) {

            int offset = LunaChat.config.getGlobalMarker().length();
            event.setMessage( event.getMessage().substring(offset) );
            chatGlobal(event);
            return;
        }

        Player player = event.getPlayer();
        Channel channel = LunaChat.manager.getDefaultChannel(player.getName());

        // デフォルトの発言先が無い場合
        if ( channel == null ) {
            if ( LunaChat.config.isNoJoinAsGlobal() ) {
                // グローバル発言にする
                chatGlobal(event);
                return;

            } else {
                // 発言をキャンセルして終了する
                event.setCancelled(true);
                return;
            }
        }

        // チャンネルチャット発言
        channel.chat(player, event.getMessage());

        // もとのイベントをキャンセル
        event.setCancelled(true);
    }

    /**
     * プレイヤーのサーバー参加ごとに呼び出されるメソッド
     * @param event プレイヤー参加イベント
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        // 強制参加チャンネル設定を確認し、参加させる
        forceJoinToForceJoinChannels(player);

        // グローバルチャンネル設定がある場合
        if ( !LunaChat.config.getGlobalChannel().equals("") ) {
            tryJoinToGlobalChannel(player);
        }

        // チャンネルチャット情報を表示する
        if ( LunaChat.config.isShowListOnJoin() ) {
            ArrayList<String> list = LunaChat.manager.getListForMotd(player);
            for ( String msg : list ) {
                player.sendMessage(msg);
            }
        }
    }

    /**
     * プレイヤーのサーバー退出ごとに呼び出されるメソッド
     * @param event プレイヤー退出イベント
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // お互いがオフラインになるPMチャンネルがある場合は
        // チャンネルをクリアする
        Collection<Channel> channels = LunaChat.manager.getChannels();
        ArrayList<Channel> deleteList = new ArrayList<Channel>();

        for ( Channel channel : channels ) {
            String cname = channel.getName();
            if ( cname.contains(">") && cname.contains(player.getName()) ) {
                boolean isAllOffline = true;
                for ( String pname : channel.getMembers() ) {
                    if ( !pname.equals(player.getName()) ) {
                        Player p = Utility.getPlayerExact(pname);
                        if ( p != null && p.isOnline() ) {
                            isAllOffline = false;
                        }
                    }
                }
                if ( isAllOffline ) {
                    deleteList.add(channel);
                }
            }
        }

        for ( Channel channel : deleteList ) {
            LunaChat.manager.removeChannel(channel.getName());
        }
    }

    /**
     * イベントをグローバルチャット発言として処理する
     * @param event 処理するイベント
     */
    private void chatGlobal(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();

        if ( !LunaChat.config.getGlobalChannel().equals("") ) {
            // グローバルチャンネル設定がある場合

            // グローバルチャンネルの取得、無ければ作成
            Channel global = LunaChat.manager.getChannel(LunaChat.config.getGlobalChannel());
            if ( global == null ) {
                global = LunaChat.manager.createChannel(LunaChat.config.getGlobalChannel());
            }

            // デフォルト発言先が無いなら、グローバルチャンネルに設定する
            Channel dchannel = LunaChat.manager.getDefaultChannel(player.getName());
            if ( dchannel == null ) {
                LunaChat.manager.setDefaultChannel(player.getName(), global.getName());
            }

            // チャンネルチャット発言
            global.chat(player, event.getMessage());

            // もとのイベントをキャンセル
            event.setCancelled(true);

            return;

        } else {
            // グローバルチャンネル設定が無い場合

            String message = event.getMessage();
            // NGワード発言をマスク
            for ( String word : LunaChat.config.getNgword() ) {
                if ( message.contains(word) ) {
                    message = message.replace(
                            word, Utility.getAstariskString(word.length()));
                }
            }

            // チャットフォーマット装飾の適用
            if ( LunaChat.config.isEnableNormalChatMessageFormat() ) {
                String format = LunaChat.config.getNormalChatMessageFormat();
                format = replaceNormalChatFormatKeywords(format, event.getPlayer());
                event.setFormat(format);
            }

            // カラーコード置き換え
            message = Utility.replaceColorCode(message);

            // 一時的にJapanizeスキップ設定かどうかを確認する
            boolean skipJapanize = false;
            String marker = LunaChat.config.getNoneJapanizeMarker();
            if ( !marker.equals("") && message.startsWith(marker) ) {
                skipJapanize = true;
                message = message.substring(marker.length());
            }

            // 2byteコードを含むなら、Japanize変換は行わない
            if ( !skipJapanize &&
                    ( message.getBytes().length > message.length() ||
                      message.matches("[ \\uFF61-\\uFF9F]+") ) ) {
                skipJapanize = true;
            }

            // Japanize変換と、発言処理
            if ( !skipJapanize &&
                    LunaChat.manager.isPlayerJapanize(player.getName()) &&
                    LunaChat.config.getJapanizeType() != JapanizeType.NONE ) {

                int lineType = LunaChat.config.getJapanizeDisplayLine();
                String taskFormat;
                if ( lineType == 1 ) {

                    taskFormat = LunaChat.config.getJapanizeLine1Format();

                    String japanized = LunaChat.manager.japanize(
                            message, LunaChat.config.getJapanizeType());
                    if ( japanized != null ) {
                        String temp = taskFormat.replace("%msg", message);
                        temp = temp.replace("%japanize", japanized);
                        message = Utility.replaceColorCode(temp);
                    }

                } else {

                    taskFormat = LunaChat.config.getJapanizeLine2Format();

                    DelayedJapanizeConvertTask task = new DelayedJapanizeConvertTask(message,
                            LunaChat.config.getJapanizeType(), null, player, taskFormat, null);

                    // 発言処理を必ず先に実施させるため、遅延を入れてタスクを実行する。
                    int wait = LunaChat.config.getJapanizeWait();
                    Bukkit.getScheduler().runTaskLater(LunaChat.instance, task, wait);
                }
            }

            event.setMessage(message);

            return;
        }
    }

    /**
     * 既定のチャンネルへの参加を試みる。
     * @param player プレイヤー
     * @return 参加できたかどうか
     */
    private boolean tryJoinToGlobalChannel(Player player) {

        String gcName = LunaChat.config.getGlobalChannel();

        // チャンネルが存在しない場合は作成する
        Channel global = LunaChat.manager.getChannel(gcName);
        if ( global == null ) {
            global = LunaChat.manager.createChannel(gcName);
        }

        // デフォルト発言先が無いなら、グローバルチャンネルに設定する
        Channel dchannel = LunaChat.manager.getDefaultChannel(player.getName());
        if ( dchannel == null ) {
            LunaChat.manager.setDefaultChannel(player.getName(), gcName);
        }

        return true;
    }

    /**
     * 強制参加チャンネルへ参加させる
     * @param player プレイヤー
     */
    private void forceJoinToForceJoinChannels(Player player) {

        List<String> forceJoinChannels = LunaChat.config.getForceJoinChannels();
        String playerName = player.getName();

        for ( String cname : forceJoinChannels ) {

            // チャンネルが存在しない場合は作成する
            Channel channel = LunaChat.manager.getChannel(cname);
            if ( channel == null ) {
                channel = LunaChat.manager.createChannel(cname);
            }

            // チャンネルのメンバーでないなら、参加する
            if ( !channel.getMembers().contains(playerName) ) {
                channel.addMember(playerName);
            }

            // デフォルト発言先が無いなら、グローバルチャンネルに設定する
            Channel dchannel = LunaChat.manager.getDefaultChannel(playerName);
            if ( dchannel == null ) {
                LunaChat.manager.setDefaultChannel(playerName, cname);
            }
        }
    }

    /**
     * 通常チャットのフォーマット設定のキーワードを置き換えして返す
     * @param org フォーマット設定
     * @param player 発言プレイヤー
     * @return キーワード置き換え済みの文字列
     */
    private String replaceNormalChatFormatKeywords(String org, Player player) {

        String format = org;
        format = format.replace("%username", "%1$s");
        format = format.replace("%msg", "%2$s");

        if ( format.contains("%prefix") || format.contains("%suffix") ) {
            String prefix = "";
            String suffix = "";
            if ( LunaChat.vaultchat != null ) {
                prefix = LunaChat.vaultchat.getPlayerPrefix(player);
                suffix = LunaChat.vaultchat.getPlayerSuffix(player);
            }
            format = format.replace("%prefix", prefix);
            format = format.replace("%suffix", suffix);
        }

        return Utility.replaceColorCode(format);

    }
}

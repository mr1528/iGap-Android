/*
* This is the source code of iGap for Android
* It is licensed under GNU AGPL v3.0
* You should have received a copy of the license in this archive (see LICENSE).
* Copyright © 2017 , iGap - www.iGap.net
* iGap Messenger | Free, Fast and Secure instant messaging application
* The idea of the RooyeKhat Media Company - www.RooyeKhat.co
* All rights reserved.
*/

package net.iGap.response;

import io.realm.Realm;
import io.realm.RealmResults;
import net.iGap.G;
import net.iGap.proto.ProtoError;
import net.iGap.proto.ProtoGroupLeft;
import net.iGap.realm.RealmClientCondition;
import net.iGap.realm.RealmClientConditionFields;
import net.iGap.realm.RealmMember;
import net.iGap.realm.RealmMemberFields;
import net.iGap.realm.RealmRoom;
import net.iGap.realm.RealmRoomFields;
import net.iGap.realm.RealmRoomMessage;
import net.iGap.realm.RealmRoomMessageFields;
import net.iGap.realm.RealmUserInfo;

public class GroupLeftResponse extends MessageHandler {

    public int actionId;
    public Object message;
    public String identity;

    public GroupLeftResponse(int actionId, Object protoClass, String identity) {
        super(actionId, protoClass, identity);

        this.message = protoClass;
        this.actionId = actionId;
        this.identity = identity;
    }

    @Override public void handler() {
        super.handler();
        final ProtoGroupLeft.GroupLeftResponse.Builder builder = (ProtoGroupLeft.GroupLeftResponse.Builder) message;
        final long roomId = builder.getRoomId();
        final long memberId = builder.getMemberId();

        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override public void execute(Realm realm) {

                RealmUserInfo realmUserInfo = realm.where(RealmUserInfo.class).findFirst();

                if (realmUserInfo != null && realmUserInfo.getUserId() == memberId) {
                    RealmRoom realmRoom = realm.where(RealmRoom.class).equalTo(RealmRoomFields.ID, roomId).findFirst();
                    if (realmRoom != null) {
                        realmRoom.deleteFromRealm();
                    }

                    RealmResults<RealmRoomMessage> realmRoomMessage = realm.where(RealmRoomMessage.class).equalTo(RealmRoomMessageFields.ROOM_ID, roomId).findAll();
                    realmRoomMessage.deleteAllFromRealm();

                    RealmClientCondition realmClientCondition = realm.where(RealmClientCondition.class).equalTo(RealmClientConditionFields.ROOM_ID, roomId).findFirst();
                    if (realmClientCondition != null) {
                        realmClientCondition.deleteFromRealm();
                    }

                    if (G.onGroupLeft != null) {
                        G.onGroupLeft.onGroupLeft(roomId, memberId);
                    }
                } else {
                    RealmRoom realmRoom = realm.where(RealmRoom.class).equalTo(RealmRoomFields.ID, roomId).findFirst();
                    if (realmRoom != null && realmRoom.getGroupRoom() != null) {

                        try {
                            RealmMember member = realmRoom.getGroupRoom().getMembers().where().equalTo(RealmMemberFields.PEER_ID, builder.getMemberId()).findFirst();
                            if (member != null) {
                                member.deleteFromRealm();
                            }
                        } catch (NullPointerException e) {

                        }
                    }
                }
            }
        });
        realm.close();
    }

    @Override public void timeOut() {
        super.timeOut();

        G.onGroupLeft.onTimeOut();
    }

    @Override public void error() {
        super.error();

        ProtoError.ErrorResponse.Builder errorResponse = (ProtoError.ErrorResponse.Builder) message;
        int majorCode = errorResponse.getMajorCode();
        int minorCode = errorResponse.getMinorCode();

        G.onGroupLeft.onError(majorCode, minorCode);
    }
}

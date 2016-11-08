package cn.ucai.superwechat.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import cn.ucai.superwechat.Constant;
import cn.ucai.superwechat.SuperWeChatApplication;
import cn.ucai.superwechat.domain.InviteMessage;
import cn.ucai.superwechat.domain.InviteMessage.InviteMesageStatus;
import cn.ucai.superwechat.domain.RobotUser;
import cn.ucai.superwechat.utils.L;

import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.util.HanziToPinyin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SuperWeChatDBManager {
    static private SuperWeChatDBManager dbMgr = new SuperWeChatDBManager();
    private DbOpenHelper dbHelper;
    private Map<String, User> appContactList;

    private SuperWeChatDBManager(){
        dbHelper = DbOpenHelper.getInstance(SuperWeChatApplication.getInstance().getApplicationContext());
    }
    
    public static synchronized SuperWeChatDBManager getInstance(){
        if(dbMgr == null){
            dbMgr = new SuperWeChatDBManager();
        }
        return dbMgr;
    }
    
    /**
     * save contact list
     * 
     * @param contactList
     */
    synchronized public void saveContactList(List<EaseUser> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(UserDao.TABLE_NAME, null, null);
            for (EaseUser user : contactList) {
                ContentValues values = new ContentValues();
                values.put(UserDao.COLUMN_NAME_ID, user.getUsername());
                if(user.getNick() != null)
                    values.put(UserDao.COLUMN_NAME_NICK, user.getNick());
                if(user.getAvatar() != null)
                    values.put(UserDao.COLUMN_NAME_AVATAR, user.getAvatar());
                db.replace(UserDao.TABLE_NAME, null, values);
            }
        }
    }

    /**
     * get contact list
     * 
     * @return
     */
    synchronized public Map<String, EaseUser> getContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, EaseUser> users = new Hashtable<String, EaseUser>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + UserDao.TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {
                String username = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_ID));
                String nick = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_NICK));
                String avatar = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_AVATAR));
                EaseUser user = new EaseUser(username);
                user.setNick(nick);
                user.setAvatar(avatar);
                if (username.equals(Constant.NEW_FRIENDS_USERNAME) || username.equals(Constant.GROUP_USERNAME)
                        || username.equals(Constant.CHAT_ROOM)|| username.equals(Constant.CHAT_ROBOT)) {
                        user.setInitialLetter("");
                } else {
                    EaseCommonUtils.setUserInitialLetter(user);
                }
                users.put(username, user);
            }
            cursor.close();
        }
        return users;
    }
    
    /**
     * delete a contact
     * @param username
     */
    synchronized public void deleteContact(String username){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            db.delete(UserDao.TABLE_NAME, UserDao.COLUMN_NAME_ID + " = ?", new String[]{username});
        }
    }
    
    /**
     * save a contact
     * @param user
     */
    synchronized public void saveContact(EaseUser user){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.COLUMN_NAME_ID, user.getUsername());
        if(user.getNick() != null)
            values.put(UserDao.COLUMN_NAME_NICK, user.getNick());
        if(user.getAvatar() != null)
            values.put(UserDao.COLUMN_NAME_AVATAR, user.getAvatar());
        if(db.isOpen()){
            db.replace(UserDao.TABLE_NAME, null, values);
        }
    }
    
    public void setDisabledGroups(List<String> groups){
        setList(UserDao.COLUMN_NAME_DISABLED_GROUPS, groups);
    }
    
    public List<String>  getDisabledGroups(){       
        return getList(UserDao.COLUMN_NAME_DISABLED_GROUPS);
    }
    
    public void setDisabledIds(List<String> ids){
        setList(UserDao.COLUMN_NAME_DISABLED_IDS, ids);
    }
    
    public List<String> getDisabledIds(){
        return getList(UserDao.COLUMN_NAME_DISABLED_IDS);
    }
    
    synchronized private void setList(String column, List<String> strList){
        StringBuilder strBuilder = new StringBuilder();
        
        for(String hxid:strList){
            strBuilder.append(hxid).append("$");
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            ContentValues values = new ContentValues();
            values.put(column, strBuilder.toString());

            db.update(UserDao.PREF_TABLE_NAME, values, null,null);
        }
    }
    
    synchronized private List<String> getList(String column){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select " + column + " from " + UserDao.PREF_TABLE_NAME,null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        String strVal = cursor.getString(0);
        if (strVal == null || strVal.equals("")) {
            return null;
        }
        
        cursor.close();
        
        String[] array = strVal.split("$");
        
        if(array.length > 0){
            List<String> list = new ArrayList<String>();
            Collections.addAll(list, array);
            return list;
        }
        
        return null;
    }
    
    /**
     * save a message
     * @param message
     * @return  return cursor of the message
     */
    public synchronized Integer saveMessage(InviteMessage message){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int id = -1;
        if(db.isOpen()){
            ContentValues values = new ContentValues();
            values.put(InviteMessgeDao.COLUMN_NAME_FROM, message.getFrom());
            values.put(InviteMessgeDao.COLUMN_NAME_GROUP_ID, message.getGroupId());
            values.put(InviteMessgeDao.COLUMN_NAME_GROUP_Name, message.getGroupName());
            values.put(InviteMessgeDao.COLUMN_NAME_REASON, message.getReason());
            values.put(InviteMessgeDao.COLUMN_NAME_TIME, message.getTime());
            values.put(InviteMessgeDao.COLUMN_NAME_STATUS, message.getStatus().ordinal());
            values.put(InviteMessgeDao.COLUMN_NAME_GROUPINVITER, message.getGroupInviter());
            db.insert(InviteMessgeDao.TABLE_NAME, null, values);
            
            Cursor cursor = db.rawQuery("select last_insert_rowid() from " + InviteMessgeDao.TABLE_NAME,null); 
            if(cursor.moveToFirst()){
                id = cursor.getInt(0);
            }
            
            cursor.close();
        }
        return id;
    }
    
    /**
     * update message
     * @param msgId
     * @param values
     */
    synchronized public void updateMessage(int msgId,ContentValues values){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            db.update(InviteMessgeDao.TABLE_NAME, values, InviteMessgeDao.COLUMN_NAME_ID + " = ?", new String[]{String.valueOf(msgId)});
        }
    }
    
    /**
     * get messges
     * @return
     */
    synchronized public List<InviteMessage> getMessagesList(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<InviteMessage> msgs = new ArrayList<InviteMessage>();
        if(db.isOpen()){
            Cursor cursor = db.rawQuery("select * from " + InviteMessgeDao.TABLE_NAME +" order by "+InviteMessgeDao.COLUMN_NAME_TIME+ " desc",null);
            while(cursor.moveToNext()){
                InviteMessage msg = new InviteMessage();
                int id = cursor.getInt(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_ID));
                String from = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_FROM));
                String groupid = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_GROUP_ID));
                String groupname = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_GROUP_Name));
                String reason = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_REASON));
                long time = cursor.getLong(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_TIME));
                int status = cursor.getInt(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_STATUS));
                String groupInviter = cursor.getString(cursor.getColumnIndex(InviteMessgeDao.COLUMN_NAME_GROUPINVITER));
                
                msg.setId(id);
                msg.setFrom(from);
                msg.setGroupId(groupid);
                msg.setGroupName(groupname);
                msg.setReason(reason);
                msg.setTime(time);
                msg.setGroupInviter(groupInviter);
                
                if(status == InviteMesageStatus.BEINVITEED.ordinal())
                    msg.setStatus(InviteMesageStatus.BEINVITEED);
                else if(status == InviteMesageStatus.BEAGREED.ordinal())
                    msg.setStatus(InviteMesageStatus.BEAGREED);
                else if(status == InviteMesageStatus.BEREFUSED.ordinal())
                    msg.setStatus(InviteMesageStatus.BEREFUSED);
                else if(status == InviteMesageStatus.AGREED.ordinal())
                    msg.setStatus(InviteMesageStatus.AGREED);
                else if(status == InviteMesageStatus.REFUSED.ordinal())
                    msg.setStatus(InviteMesageStatus.REFUSED);
                else if(status == InviteMesageStatus.BEAPPLYED.ordinal())
                    msg.setStatus(InviteMesageStatus.BEAPPLYED);
                else if(status == InviteMesageStatus.GROUPINVITATION.ordinal())
                    msg.setStatus(InviteMesageStatus.GROUPINVITATION);
                else if(status == InviteMesageStatus.GROUPINVITATION_ACCEPTED.ordinal())
                    msg.setStatus(InviteMesageStatus.GROUPINVITATION_ACCEPTED);
                else if(status == InviteMesageStatus.GROUPINVITATION_DECLINED.ordinal())
                    msg.setStatus(InviteMesageStatus.GROUPINVITATION_DECLINED);
                
                msgs.add(msg);
            }
            cursor.close();
        }
        return msgs;
    }
    
    /**
     * delete invitation message
     * @param from
     */
    synchronized public void deleteMessage(String from){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            db.delete(InviteMessgeDao.TABLE_NAME, InviteMessgeDao.COLUMN_NAME_FROM + " = ?", new String[]{from});
        }
    }
    
    synchronized int getUnreadNotifyCount(){
        int count = 0;
        L.e("MSG_COUNT","MSG_COUNT"+count);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if(db.isOpen()){
            Cursor cursor = db.rawQuery("select " + InviteMessgeDao.COLUMN_NAME_UNREAD_MSG_COUNT + " from " + InviteMessgeDao.TABLE_NAME, null);
            if(cursor.moveToFirst()){
                count = cursor.getInt(0);
                L.e("MSG_COUNT","MSG_COUNT"+count);
            }
            cursor.close();
        }
         return count;
    }
    
    synchronized void setUnreadNotifyCount(int count){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if(db.isOpen()){
            ContentValues values = new ContentValues();
            values.put(InviteMessgeDao.COLUMN_NAME_UNREAD_MSG_COUNT, count);

            db.update(InviteMessgeDao.TABLE_NAME, values, null,null);
        }
    }
    
    synchronized public void closeDB(){
        if(dbHelper != null){
            dbHelper.closeDB();
        }
        dbMgr = null;
    }
    
    
    /**
     * Save Robot list
     */
	synchronized public void saveRobotList(List<RobotUser> robotList) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (db.isOpen()) {
			db.delete(UserDao.ROBOT_TABLE_NAME, null, null);
			for (RobotUser item : robotList) {
				ContentValues values = new ContentValues();
				values.put(UserDao.ROBOT_COLUMN_NAME_ID, item.getUsername());
				if (item.getNick() != null)
					values.put(UserDao.ROBOT_COLUMN_NAME_NICK, item.getNick());
				if (item.getAvatar() != null)
					values.put(UserDao.ROBOT_COLUMN_NAME_AVATAR, item.getAvatar());
				db.replace(UserDao.ROBOT_TABLE_NAME, null, values);
			}
		}
	}
    
    /**
     * load robot list
     */
	synchronized public Map<String, RobotUser> getRobotList() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Map<String, RobotUser> users = null;
		if (db.isOpen()) {
			Cursor cursor = db.rawQuery("select * from " + UserDao.ROBOT_TABLE_NAME, null);
			if(cursor.getCount()>0){
				users = new Hashtable<String, RobotUser>();
			}
            while (cursor.moveToNext()) {
				String username = cursor.getString(cursor.getColumnIndex(UserDao.ROBOT_COLUMN_NAME_ID));
				String nick = cursor.getString(cursor.getColumnIndex(UserDao.ROBOT_COLUMN_NAME_NICK));
				String avatar = cursor.getString(cursor.getColumnIndex(UserDao.ROBOT_COLUMN_NAME_AVATAR));
				RobotUser user = new RobotUser(username);
				user.setNick(nick);
				user.setAvatar(avatar);
				String headerName = null;
				if (!TextUtils.isEmpty(user.getNick())) {
					headerName = user.getNick();
				} else {
					headerName = user.getUsername();
				}
				if(Character.isDigit(headerName.charAt(0))){
					user.setInitialLetter("#");
				}else{
					user.setInitialLetter(HanziToPinyin.getInstance().get(headerName.substring(0, 1)).get(0).target
							.substring(0, 1).toUpperCase());
					char header = user.getInitialLetter().toLowerCase().charAt(0);
					if (header < 'a' || header > 'z') {
						user.setInitialLetter("#");
					}
				}

                try {
                    users.put(username, user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
			}
			cursor.close();
		}
		return users;
	}

    //  实现在本地数据库保存用户，得到用户，更新用户的方法

    public synchronized boolean saveUserData(User user){
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.USER_NAME,user.getMUserName());
        values.put(UserDao.USER_NICK,user.getMUserNick());
        values.put(UserDao.USER_AVATAR_ID,user.getMAvatarId());
        values.put(UserDao.USER_AVATAR_PATH,user.getMAvatarPath());
        values.put(UserDao.USER_AVATAR_SUFFIX,user.getMAvatarSuffix());
        values.put(UserDao.USER_AVATAR_TYPE,user.getMAvatarType());
        values.put(UserDao.USER_AVATAR_LASTUPDATETIME,user.getMAvatarLastUpdateTime());
        if(database.isOpen()){
            return database.replace(UserDao.USER_TABLE,null,values)!=-1;
        }
        return false;
    }
    public User getUser(String userName){
        String sql = "select*from "+UserDao.USER_TABLE+" where "+UserDao.USER_NAME+" = ?";
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cs = database.rawQuery(sql, new String[]{userName});
        User user = null;
        if(cs.moveToNext()){
            user = new User();
            user.setMUserName(cs.getString(cs.getColumnIndex(UserDao.USER_NAME)));
            user.setMUserNick(cs.getString(cs.getColumnIndex(UserDao.USER_NICK)));
            user.setMAvatarId(cs.getInt(cs.getColumnIndex(UserDao.USER_AVATAR_ID)));
            user.setMAvatarPath(cs.getString(cs.getColumnIndex(UserDao.USER_AVATAR_PATH)));
            user.setMAvatarType(cs.getInt(cs.getColumnIndex(UserDao.USER_AVATAR_TYPE)));
            user.setMAvatarSuffix(cs.getString(cs.getColumnIndex(UserDao.USER_AVATAR_SUFFIX)));
            user.setMAvatarLastUpdateTime(cs.getString(cs.getColumnIndex(UserDao.USER_AVATAR_LASTUPDATETIME)));
            return user;
        }
        return user;
    }
    public synchronized boolean updateUser(User user){
        int result = -1;
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        String sql = UserDao.USER_NAME +"=?";
        ContentValues values = new ContentValues();
        values.put(UserDao.USER_NICK,user.getMUserNick());
        if(database.isOpen()){
            result = database.update(UserDao.USER_TABLE,values,sql,new String[]{user.getMUserName()});
        }
        return result>0;
    }

    /**
     * save a contact
     * @param user
     */
    synchronized public void saveAppContact(User user){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.USER_NAME, user.getMUserName());
        if(user.getMUserNick() != null)
            values.put(UserDao.USER_NICK, user.getMUserNick());
        if(user.getMAvatarId() != null)
            values.put(UserDao.USER_AVATAR_ID,user.getMAvatarId());
        if(user.getMAvatarPath() != null)
            values.put(UserDao.USER_AVATAR_PATH,user.getMAvatarPath());
        if(user.getMAvatarSuffix() != null)
            values.put(UserDao.USER_AVATAR_SUFFIX,user.getMAvatarSuffix());
        if(user.getMAvatarType() != null)
            values.put(UserDao.USER_AVATAR_TYPE,user.getMAvatarType());
        if(user.getMAvatarLastUpdateTime() != null)
            values.put(UserDao.USER_AVATAR_LASTUPDATETIME,user.getMAvatarLastUpdateTime());
        db.replace(UserDao.USER_TABLE, null, values);
    }

    synchronized public Map<String, User> getAppContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, User> users = new Hashtable<String, User>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + UserDao.USER_TABLE /* + " desc" */, null);
            while (cursor.moveToNext()) {
                String username = cursor.getString(cursor.getColumnIndex(UserDao.USER_NAME));
                String nick = cursor.getString(cursor.getColumnIndex(UserDao.USER_NICK));
                int avatarId = cursor.getInt(cursor.getColumnIndex(UserDao.USER_AVATAR_ID));
                String avatarPath = cursor.getString(cursor.getColumnIndex(UserDao.USER_AVATAR_PATH));
                int avatarType = cursor.getInt(cursor.getColumnIndex(UserDao.USER_AVATAR_TYPE));
                String avatarSuffix = cursor.getString(cursor.getColumnIndex(UserDao.USER_AVATAR_SUFFIX));
                String avatarLastUpdateTime = cursor.getString(cursor.getColumnIndex(UserDao.USER_AVATAR_LASTUPDATETIME));
                User user = new User(username);
                user.setMUserNick(nick);
                user.setMAvatarId(avatarId);
                user.setMAvatarSuffix(avatarSuffix);
                user.setMAvatarPath(avatarPath);
                user.setMAvatarType(avatarType);
                user.setMAvatarLastUpdateTime(avatarLastUpdateTime);
                if (username.equals(Constant.NEW_FRIENDS_USERNAME) || username.equals(Constant.GROUP_USERNAME)
                        || username.equals(Constant.CHAT_ROOM)|| username.equals(Constant.CHAT_ROBOT)) {
                    user.setInitialLetter("");
                } else {
                    EaseCommonUtils.setAppUserInitialLetter(user);
                }
                users.put(username, user);
            }
            cursor.close();
        }
        return users;
    }

    synchronized public void saveAppContactList(List<User> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(UserDao.USER_TABLE, null, null);
            for (User user : contactList) {
                ContentValues values = new ContentValues();
                values.put(UserDao.USER_NAME, user.getMUserName());
                if(user.getMUserNick() != null)
                    values.put(UserDao.USER_NICK, user.getMUserNick());
                if(user.getMAvatarId() != null)
                    values.put(UserDao.USER_AVATAR_ID,user.getMAvatarId());
                if(user.getMAvatarPath() != null)
                    values.put(UserDao.USER_AVATAR_PATH,user.getMAvatarPath());
                if(user.getMAvatarSuffix() != null)
                    values.put(UserDao.USER_AVATAR_SUFFIX,user.getMAvatarSuffix());
                if(user.getMAvatarType() != null)
                    values.put(UserDao.USER_AVATAR_TYPE,user.getMAvatarType());
                if(user.getMAvatarLastUpdateTime() != null)
                    values.put(UserDao.USER_AVATAR_LASTUPDATETIME,user.getMAvatarLastUpdateTime());
                db.replace(UserDao.USER_TABLE, null, values);
            }
        }
    }
}

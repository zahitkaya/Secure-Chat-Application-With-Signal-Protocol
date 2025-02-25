package com.example.chat_app;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.example.chat_app.adapters.ChatAdapter;
import com.example.chat_app.model.KeyPairsMaker;
import com.example.chat_app.model.Message;
import com.example.chat_app.model.PreKeyBundleMaker;
import com.example.chat_app.model.StoreMaker;
import com.example.chat_app.signal.Session;
import com.example.chat_app.util.ByteConverter;
import com.example.chat_app.util.InMemorySignalProtocolStoreCreatorUtil;
import com.example.chat_app.util.PreKeyBundleCreatorUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.Curve;
import org.whispersystems.libsignal.ecc.ECKeyPair;
import org.whispersystems.libsignal.ecc.ECPrivateKey;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import lombok.SneakyThrows;

public class ChatActivity extends AppCompatActivity {

    private EditText messageEditText;
    private TextView userName,chatIsOnline;
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private ImageView isReaded;
    private ListView listView;
    private ArrayList<String> messageList=new ArrayList<>();
    private ArrayList<String> userList=new ArrayList<>();
    private ArrayList<String> messageTimeList=new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Session aliceToBobSession;
    private PreKeyBundle bobPreKeyBundle,alicePreKeyBundle;
    private SignalProtocolStore signalProtocolStore;
    private SignalProtocolAddress signalProtocolAddress;
    private SQLiteDatabase database;
    public static PreKeyRecord preKeyRecord;
    public static SignedPreKeyRecord signedPreKeyRecord;


    @RequiresApi(api=Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        database=this.openOrCreateDatabase("Privates",MODE_PRIVATE,null);

        messageEditText=findViewById(R.id.messageEditText);
        userName=findViewById(R.id.chat_tool_bar_user_name);
        chatIsOnline=findViewById(R.id.chat_is_online);
        mAuth=FirebaseAuth.getInstance();
        listView=findViewById(R.id.messageListView);
        isReaded=findViewById(R.id.isReaded);

        String senderEmail=mAuth.getCurrentUser().getEmail();
        String senderUid=mAuth.getCurrentUser().getUid();

        String receiverEmail=getIntent().getStringExtra("RECEIVER_EMAIL");
        String receiverUid=getIntent().getStringExtra("RECEIVER_UID");
        String nameSurname=getIntent().getStringExtra("RECEIVER_NAME");
        database.execSQL("CREATE TABLE IF NOT EXISTS '"+sortUid(senderUid,receiverUid)+"' (message VARCHAR,receiver VARCHAR, sender VARCHAR,msgTimeStamp VARCHAR)");

        userName.setText(nameSurname);

        if(aliceToBobSession==null) {

            FirebaseDatabase.getInstance().getReference("Users").child(receiverUid).addValueEventListener(new ValueEventListener() {
                @RequiresApi(api=Build.VERSION_CODES.O)
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    PreKeyBundleMaker preKeyBundleMaker=snapshot.child("preKeyBundleMaker").getValue(PreKeyBundleMaker.class);
                    bobPreKeyBundle=PreKeyBundleCreatorUtil.createPreKeyBundle(preKeyBundleMaker);
                    if (snapshot.child("online").getValue(Boolean.class)){
                        chatIsOnline.setText("Online");
                    }
                    else {
                        chatIsOnline.setText("Offline");
                    }

                    Log.d("TAG","onDataChange: ");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            FirebaseDatabase.getInstance().getReference("Users").child(senderUid).addValueEventListener(new ValueEventListener() {
                @RequiresApi(api=Build.VERSION_CODES.O)
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    PreKeyBundleMaker preKeyBundleMaker=snapshot.child("preKeyBundleMaker").getValue(PreKeyBundleMaker.class);
                    alicePreKeyBundle=PreKeyBundleCreatorUtil.createPreKeyBundle(preKeyBundleMaker);

                    String id,storeMakerString=null,keyPairMakerString=null;
                    Cursor c=database.rawQuery("SELECT * FROM SignalPrivates WHERE id='"+senderUid+"' ",null);
                    if (c.moveToFirst()){
                        do {
                            // Passing values
                            id = c.getString(0);
                            storeMakerString = c.getString(1);
                            StoreMaker storeMaker=ByteConverter.readStore(Base64.getDecoder().decode(storeMakerString));
                            signalProtocolStore=InMemorySignalProtocolStoreCreatorUtil.createStore(storeMaker);


                            keyPairMakerString = c.getString(2);
                            KeyPairsMaker keyPairsMaker=ByteConverter.readKeyPairs(Base64.getDecoder().decode(keyPairMakerString));
                            byte[] decodedPrivateKey=Base64.getDecoder().decode(keyPairsMaker.getPreKeyPairPrivateKey());
                            ECPrivateKey ecPrivateKey=Curve.decodePrivatePoint(decodedPrivateKey);
                            ECKeyPair ecKeyPair=new ECKeyPair(alicePreKeyBundle.getPreKey(),ecPrivateKey);
                            preKeyRecord=new PreKeyRecord(alicePreKeyBundle.getPreKeyId(),ecKeyPair);

                            byte[] decodedSignedPrivateKey=Base64.getDecoder().decode(keyPairsMaker.getSignedPreKeySignaturePrivateKey());
                            ECPrivateKey signedPrivateKey=Curve.decodePrivatePoint(decodedSignedPrivateKey);
                            ECKeyPair signedPreKeyPair=new ECKeyPair(alicePreKeyBundle.getSignedPreKey(),signedPrivateKey);

                            signedPreKeyRecord=new SignedPreKeyRecord(
                                    alicePreKeyBundle.getSignedPreKeyId(),keyPairsMaker.getTimestamp(),signedPreKeyPair,alicePreKeyBundle.getSignedPreKeySignature());

                            signalProtocolStore.storePreKey(alicePreKeyBundle.getPreKeyId(),preKeyRecord);
                            signalProtocolStore.storeSignedPreKey(alicePreKeyBundle.getSignedPreKeyId(),signedPreKeyRecord);

                            signalProtocolAddress=new SignalProtocolAddress(receiverUid,1);

                            aliceToBobSession=new Session(signalProtocolStore,bobPreKeyBundle,signalProtocolAddress);

                            // Do something Here with values
                        } while(c.moveToNext());
                    }
                    c.close();


                    Log.d("TAG","onDataChange: ");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

            String sortUid=sortUid(receiverUid,senderUid);

            adapter=new ChatAdapter(ChatActivity.this,messageList,messageTimeList,userList,sortUid);


        ArrayList<String> previousCipherText=new ArrayList<>();

        FirebaseDatabase.getInstance().getReference("Messages").child(sortUid).addValueEventListener(new ValueEventListener() {

            @SneakyThrows
            @RequiresApi(api=Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message message=dataSnapshot.getValue(Message.class);

                    String cipherText=message.getMessage(); //Şifreli şu anda decryption etmemiz gerek.

                    if (message.getReceiver().equals(mAuth.getCurrentUser().getEmail())&& !message.getDecrypted() && !previousCipherText.contains(cipherText)) {
                        byte[] ds=Base64.getDecoder().decode(cipherText);
                        PreKeySignalMessage toBobMessageDecrypt = new PreKeySignalMessage(ds);
                        previousCipherText.add(cipherText);

                        signalProtocolStore.storePreKey(alicePreKeyBundle.getPreKeyId(),preKeyRecord);
                        signalProtocolStore.storeSignedPreKey(alicePreKeyBundle.getSignedPreKeyId(),signedPreKeyRecord);

                        signalProtocolAddress=new SignalProtocolAddress(receiverUid,1);

                        aliceToBobSession=new Session(signalProtocolStore,bobPreKeyBundle,signalProtocolAddress);

                        String plainText=aliceToBobSession.decrypt(toBobMessageDecrypt);
                        message.setMessage(plainText);
                        message.setDecrypted(true);

                        FirebaseDatabase.getInstance().getReference("Messages").child(sortUid).child(message.getMsgTimeStamp()).setValue(message);
                        //REMOVE DENIYCEGIM !!!!!!!!!!!!!!!!!!!!!!
                      FirebaseDatabase.getInstance().getReference("Messages").child(sortUid).child(message.getMsgTimeStamp()).removeValue();

                        database.execSQL("INSERT INTO  '"+sortUid+"' (message,receiver, sender,msgTimeStamp) " +
                                "VALUES ('"+plainText+"','"+mAuth.getCurrentUser().getEmail()+"','"+ receiverEmail+"','"+message.getMsgTimeStamp()+"')");

                        selectAllMessagesFromDb(messageList,userList,sortUid);

                        adapter.notifyDataSetChanged();

                    }

                }

                }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        selectAllMessagesFromDb(messageList,userList,sortUid);

        listView.setAdapter(adapter);


    }

    @RequiresApi(api=Build.VERSION_CODES.O)
    public void sendMessageButton(View view) throws IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        String receiverUid=getIntent().getStringExtra("RECEIVER_UID");
        String receiverEmail=getIntent().getStringExtra("RECEIVER_EMAIL");

        String sortedUid=sortUid(receiverUid,mAuth.getUid().toString());
        String timestamp=Long.toString(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(3)).getTime()); //mesajın zamanı için türkiye saati için 3 saat ekledik

        String messageText=messageEditText.getText().toString();
     //   String cipherMessage=Base64.getEncoder().encodeToString(encrypt(messageText,receiverPublicKey));
        PreKeySignalMessage toBobMessage = aliceToBobSession.encrypt(messageText);
        String signalCipherText=Base64.getEncoder().encodeToString(toBobMessage.serialize());

        Message message=Message.builder()
                .message(signalCipherText)
                .receiver(receiverEmail)
                .sender(mAuth.getCurrentUser().getEmail())
                .msgTimeStamp(timestamp)
                .decrypted(false)
                .build();

        //GÖNDERİLEN MESAJLARI DB YE ATTIK


        database.execSQL("INSERT INTO  '"+sortedUid+"' (message,receiver, sender,msgTimeStamp) " +
                "VALUES ('"+messageText+"','"+receiverEmail+"','"+ mAuth.getCurrentUser().getEmail()+"','"+timestamp+"')");



        FirebaseDatabase.getInstance().getReference("Messages").child(sortedUid).child(timestamp).setValue(message);

        selectAllMessagesFromDb(messageList,userList,sortedUid);

        adapter.notifyDataSetChanged();

        messageEditText.setText("");
        messageEditText.setText("");
    }
//asdasd
    @RequiresApi(api=Build.VERSION_CODES.N)
    public String sortUid(String sender,String receiver) {
        String result=sender + receiver;
        String sorted=result.chars()
                .sorted()
                .collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append)
                .toString();
        return sorted;
    }

    public void selectAllMessagesFromDb(List messageList,List userList,String sortUid){
        userList.clear();
        messageList.clear();
        messageTimeList.clear();

        Cursor cursor=database.rawQuery("SELECT * FROM '" + sortUid + "'",null);
        while (cursor.moveToNext()) {
            if (messageTimeList.size()==0){
                messageList.add(cursor.getString(0));
                userList.add(cursor.getString(1));
                //    String msgTimeStamp=cursor.getString(2);
                //   int day = (int) TimeUnit.SECONDS.toHours(Integer.valueOf(msgTimeStamp));
                messageTimeList.add(cursor.getString(3));
            }
            else {
                if( !cursor.getString(3).equals(messageTimeList.get(messageTimeList.size()-1))){
                    messageList.add(cursor.getString(0));
                    userList.add(cursor.getString(1));
                    //    String msgTimeStamp=cursor.getString(2);
                    //   int day = (int) TimeUnit.SECONDS.toHours(Integer.valueOf(msgTimeStamp));
                    messageTimeList.add(cursor.getString(3));
                }
            }
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }


    public void goBack(View view) {
        Intent intent = new Intent(ChatActivity.this,HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
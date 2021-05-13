package com.example.chat_app.adapters;

import android.content.Context;
import android.media.Image;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.chat_app.R;
import com.example.chat_app.model.Message;
import com.example.chat_app.rsa.Session;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import lombok.SneakyThrows;

public class ChatAdapter extends ArrayAdapter<String>
{
    private Context context;
    private List<String> strings; //message
    private List<String> strings1; //messagetype
    private List<String> userList; //userList
    private List<String> allTimeStamps=new ArrayList<>();
    private String sortedUid;


    public ChatAdapter(Context context, List<String> strings,List<String> strings1,List<String> userList,String sortedUid)
    {
        super(context, R.layout.activity_chat,strings);
        this.context = context;

        this.strings = new ArrayList<String>();
        this.strings = strings;

        this.strings1 = new ArrayList<String>();
        this.strings1 = strings1;

        this.userList = new ArrayList<String>();
        this.userList = userList;

        this.sortedUid=new String();
        this.sortedUid=sortedUid;

    }

    @Override
    public View getView(int position,View convertView,ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        if (strings.size()>position) {
            Timestamp ts=new Timestamp(Long.parseLong(strings1.get(position)));
            Date date=ts;
            DateFormat dateFormat = new SimpleDateFormat("HH:mm");


            if (!FirebaseAuth.getInstance().getCurrentUser().getEmail().equals(userList.get(position))){
                View rowView = inflater.inflate(R.layout.chat_message_me, parent, false);

                TextView your_first_text_view = (TextView) rowView.findViewById(R.id.text_gchat_message_me);
                TextView your_second_text_view = (TextView) rowView.findViewById(R.id.text_gchat_timestamp_me);
                ImageView isReaded = (ImageView) rowView.findViewById(R.id.isReaded);
                isReaded.setImageResource(R.drawable.ic_baseline_done_all_24_green);
                your_first_text_view.setText(strings.get(position));
                your_second_text_view.setText(dateFormat.format(date));
                allTimeStamps.add(strings1.get(position));
                if (position!=0 && strings1.get(position).equals(strings1.get(position-1))){
                    position++;
                    rowView.setVisibility(View.GONE);
                }
                findIsReadedMessages(position,isReaded);
                return rowView;
            }
            else{
                View rowView = inflater.inflate(R.layout.chat_message_other, parent, false);
                TextView your_first_text_view = (TextView) rowView.findViewById(R.id.text_gchat_message_other);
                TextView your_second_text_view = (TextView) rowView.findViewById(R.id.text_gchat_timestamp_other);
                your_first_text_view.setText(strings.get(position));
                your_second_text_view.setText(dateFormat.format(date));
                allTimeStamps.add(strings1.get(position));
                if (position!=0 && strings1.get(position).equals(strings1.get(position-1))){
                    rowView.setVisibility(View.GONE);
                }
                return rowView;
            }

        }

        return null;

    }

    public void findIsReadedMessages(int position,ImageView imageView){
        FirebaseDatabase.getInstance().getReference("Messages").child(sortedUid).addValueEventListener(new ValueEventListener() {

            @SneakyThrows
            @RequiresApi(api=Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message message=dataSnapshot.getValue(Message.class);

                    if (message.getMsgTimeStamp().equals(allTimeStamps.get(position))) {
                        imageView.setImageResource(R.drawable.ic_baseline_done_all_24);
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


}
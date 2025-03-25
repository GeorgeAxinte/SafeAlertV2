package com.example.safealertv2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ScheduleMessageActivity extends AppCompatActivity {

    private EditText editTextTime, editTextPhoneNumber, editTextMessage;
    private Button scheduleButton;
    private ScheduledMessage scheduledMessageSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_message);

        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextTime = findViewById(R.id.editTextTime);
        scheduleButton = findViewById(R.id.scheduleButton);

        scheduledMessageSender = new ScheduledMessage(this);

        scheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phoneNumber = editTextPhoneNumber.getText().toString().trim();
                String message = editTextMessage.getText().toString().trim();
                String delayStr = editTextTime.getText().toString().trim();
                if (!phoneNumber.isEmpty() && !message.isEmpty() && !delayStr.isEmpty()) {
                    try {
                        long delayMillis = Long.parseLong(delayStr) * 1000;
                        scheduledMessageSender.scheduleMessage(phoneNumber, message, delayMillis);
                    } catch (NumberFormatException e) {
                        Toast.makeText(ScheduleMessageActivity.this, "Enter a valid number for time!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ScheduleMessageActivity.this, "Fill in the number, message, and waiting time!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
package com.example.emergencylocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LocationListenerInterface {

    private Button button_sos;
    private Button button_add;
    private static final int RESULT_PICK_CODE = 1;
    private static final int PERMISSION_CODE = 1000;

    private boolean isLocationStreamerEnabled;
    private ConstraintLayout appContainer;
    private SmsManager smsManager;
    private ArrayList<Contact> contactList;
    private LocationManager locationManager;
    private MyLocationListener myLocListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        isLocationStreamerEnabled = false;
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        myLocListener = new MyLocationListener();
        myLocListener.setLocationListenerInterface(this);
        button_sos = findViewById(R.id.buttonSOS);
        button_sos.setOnClickListener(view -> onSosClick(view));
        button_add = findViewById(R.id.buttonAddContact);
        button_add.setOnClickListener(view -> onAddClick(view));
        contactList = new ArrayList<>();
        appContainer = findViewById(R.id.appContainer);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(this, "Нет доступа к некоторым правам", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void checkPermissions() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30, 1, myLocListener);
    }

    protected void onSosClick(View view) {
        if (contactList.size() > 0)
        {
            int colorFrom = getResources().getColor(R.color.light_red);
            int colorTo = getResources().getColor(R.color.light_green);

            isLocationStreamerEnabled = !isLocationStreamerEnabled;
            TextView textView = (TextView) findViewById(R.id.textViewStatus);
            if (isLocationStreamerEnabled)
            {
                textView.setText("Передача местоположения включена. Нажмите на кнопку, чтобы выключить");
                colorTransition(colorTo, colorFrom);
                button_sos.setBackgroundResource(R.drawable.on_button);
                button_sos.setText("ON");
                containsAllPermissions();
                sendSmsToContactList("Абонент включил трансляцию местоположения");
                checkPermissions();

            }
            else
            {
                textView.setText("Передача местоположения отключена. Нажмите на кнопку, чтобы включить");
                button_sos.setBackgroundResource(R.drawable.sos_button);
                colorTransition(colorFrom, colorTo);
                button_sos.setText("S.O.S");
            }
        }
        else
        {
            Toast.makeText(this, "Нет контактов для транслирования местоположения", Toast.LENGTH_SHORT).show();
        }
    }


    protected void onAddClick(View view){
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
    {
        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_CODE);
    }
    else{
        Intent in = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(in, RESULT_PICK_CODE);
    }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_PICK_CODE:
                    contactPicked(data);
                    break;
            }
        } else {
            Toast.makeText(this, "Не удалось выбрать контакт", Toast.LENGTH_SHORT).show();
        }
    }

    private void contactPicked(Intent data) {
        Cursor cursor = null;
        try{
            String phone = null;
            Uri uri = data.getData();
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY);
            String number = cursor.getString(phoneIndex);
            String name = cursor.getString(nameIndex);
            Contact contact = new Contact(name, number);
            if(!containsDuplicate(contact))
            {
                contactList.add(contact);
                Toast.makeText(this, "Выбранный контакт добавлен для транслирования местоположения", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "Данный контакт уже добавлен", Toast.LENGTH_SHORT).show();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    protected boolean containsDuplicate(Contact contact){
        for(Contact currentContact : contactList) {
            if (currentContact.getName().equals(contact.getName()) && currentContact.getNumber().equals(contact.getNumber())){
                return true;
            }
        }
        return false;
    }
    protected void containsAllPermissions(){
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void colorTransition(int colorFrom, int colorTo){
        ValueAnimator colorAnimation;
        colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorTo, colorFrom);
        colorAnimation.setDuration(250); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                appContainer.setBackgroundColor((int) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(isLocationStreamerEnabled)
        {
            String text = "Текущее местоположение:\nШирота: " + location.getLatitude() + "\nДолгота: " + location.getLongitude();
            sendSmsToContactList(text);
        }
    }

    protected void sendSmsToContactList(String text){
        smsManager = SmsManager.getDefault();
        for(Contact contact : contactList)
        {
            smsManager.sendTextMessage(contact.getNumber().replaceAll("\\D+", ""),null, text, null, null);
        }
    }
}
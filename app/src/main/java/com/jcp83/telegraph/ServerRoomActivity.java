package com.jcp83.telegraph;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class ServerRoomActivity extends AppCompatActivity
{
    private final int PORT = 7000;
    private Server _Server = null;
    private Thread _ServerThread = null;
    private TextView _MessagesBox = null;
    private ScrollView _ServerMessagesBoxScrollView = null;
    private TextView _StatusTextView = null;
    private EditText _MessageBox = null;
    private ToggleButton _VisibilityToggleButton = null;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_room);
        _MessagesBox = (TextView)findViewById(R.id.ServerMessagesBox);
        _ServerMessagesBoxScrollView = (ScrollView)findViewById(R.id.ServerMessagesBoxScrollView);
        _StatusTextView = (TextView)findViewById(R.id.ServerStatusTextView);
        _MessageBox = (EditText)findViewById(R.id.ServerMessageBox);
        _StatusesStack.add(Status.SERVER_IDLE);
        _StatusTextView.setText(GetStringStatus(Status.SERVER_IDLE));
        _VisibilityToggleButton = (ToggleButton)findViewById(R.id.ServerVisibilityToggleButton);
        _VisibilityToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                if(b) StartConnector(); else StopConnector();
            }
        });
    }
    @Override
    protected void onStart()
    {
        super.onStart();
        new LockOrientation(this);
        Intent _Intent = getIntent();
        _Server = new Server(this, PORT, _Intent.getStringExtra(RoomPresetActivity.RoomNameIntentID));
        _ServerThread = new Thread(_Server);
        StartServer();
        boolean _Visibility = _Intent.getBooleanExtra(RoomPresetActivity.RoomStartVisibilityIntentID, true);
        if(_Visibility) StartConnector();
        _VisibilityToggleButton.setChecked(_Visibility);
    }
    private void ScrollMessagesBoxScrollView()
    {
        _ServerMessagesBoxScrollView.post(new Runnable() {
            @Override
            public void run() {
                _ServerMessagesBoxScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
    protected void Exit()
    {
        _Server.Stop();
        while(!_Server.Stopped());
        startActivity(new Intent(ServerRoomActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
    public void UploadFileButtonClick(View view) { F(); }
    protected void UploadFile(String Path)
    {
        _Server.UploadFile(Path);
    }
    protected void F()
    {
        OpenFileDialog _OpenFileDialog = new OpenFileDialog(this);
        _OpenFileDialog._ServerRoomActivity = this;
        _OpenFileDialog.show();
    }
    protected void SetRoomNameToStatus()
    {
        _StatusTextView.setText(_Server._RoomName);
    }
    private void StartConnector()
    {
        _Server.StartConnector();
    }
    private void StopConnector()
    {
        _Server.StopConnector();
    }
    private void StartServer()
    {
        if(_Server.Started()) return;
        _ServerThread.start();
        while(!_Server.Started());
    }
    public void ExitFromServerRoomButtonClick(View view) { Exit(); }
    void ShowMessage(String Msg)
    {
        new Thread(new ShowMessage(Msg)).start();
    }
    public void ServerSendMessageButtonClick(View view)
    {
        if(!_Server.Started()) return;
        _Server.SendText(_MessageBox.getText().toString());
        _MessageBox.setText("");
    }
    class ShowMessage implements Runnable
    {
        final String Msg;
        public void run()
        {
            _MessagesBox.post(new Runnable()
            {
                public void run()
                {
                    _MessagesBox.append(Msg);
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                    ScrollMessagesBoxScrollView();
                }
            });
        }
        public ShowMessage(String Msg) { this.Msg = Msg; }
    }
    private String GetStringStatus(Status _Status)
    {
        switch(_Status)
        {
            case SERVER_IDLE:if(_Server==null||_Server._RoomName==null)
                return getString(R.string.Status_SERVER_IDLE_Text);
                else return _Server._RoomName;
            case SERVER_STARTING:return getString(R.string.Status_SERVER_STARTING_Text);
            case SERVER_STOPPING:return getString(R.string.Status_SERVER_STOPPING_Text);
            case SERVER_CONNECTOR_STARTING:return getString(R.string.Status_SERVER_CONNECTOR_STARTING_Text);
            case SERVER_CONNECTOR_STOPPING:return getString(R.string.Status_SERVER_CONNECTOR_STOPPING_Text);
            default: return "";
        }
    }
    private final ArrayList<Status> _StatusesStack = new ArrayList<>();
    void PushStatus(Status _Status)
    {
        _StatusesStack.add(0,_Status);
        _SetStatus(_Status);
    }
    void PopStatus()
    {
        if(_StatusesStack.size()>1) _StatusesStack.remove(0);
        _SetStatus(_StatusesStack.get(0));
    }
    private void _SetStatus(Status _Status)
    {
        new Thread(new SetStatus(GetStringStatus(_Status))).start();
    }
    class SetStatus implements Runnable
    {
        final String _Status;
        public void run()
        {
            _MessagesBox.post(new Runnable()
            {
                public void run()
                {
                    _StatusTextView.setText(_Status);
                }
            });
        }
        public SetStatus(String _Status) { this._Status = _Status; }
    }
}

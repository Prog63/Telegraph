package com.jcp83.telegraph;

import java.io.IOException;
import java.net.Socket;

public class ServerAccepter implements Runnable
{
    final int Timeout=30000;
    private Server _Server;
    private Socket _Socket;
    private ServerRoomActivity _ServerRoomActivity;
    private ServerListener _ServerListener;
    private ServerSender _ServerSender;
    private String Login;
    public ServerAccepter(Server _Server, Socket _Socket, ServerRoomActivity _ServerRoomActivity)
    {
        this._Server = _Server;
        this._Socket = _Socket;
        this._ServerRoomActivity = _ServerRoomActivity;
    }
    private boolean _Started = false;
    protected boolean Started() { return _Started; }
    protected void Log(String Msg)
    {
        _ServerRoomActivity.ShowMessage(Msg);
    }
    private boolean _Failed = false;
    private void Fail()
    {
        Log("\tClientSender failed.");
        _Failed = true;
    }
    private void Accept()
    {
        Log("Accepting new client ...");
        try
        {
            _Socket.setSoTimeout(0);
            String Buf;
            Package LOGIN_P=(Package)_ServerListener.Get();
            Login = LOGIN_P._Data;
            Log("Client login : "+Login);
            Package LOGIN_PASSWORD_P=(Package)_ServerListener.Get();
            Buf=LOGIN_PASSWORD_P._Data;
            if(LOGIN_P._Command!= Command.LOGIN||LOGIN_PASSWORD_P._Command!=Command.LOGIN_PASSWORD) {Log("Incorrect login signature."); Fail(); return; }
            if(Buf.compareTo("1234")!=0)
            {
                Log("Incorrect password !");
                Package LOGIN_FAILED_P = new Package(Command.LOGIN_FAILED, "");
                _ServerSender.Send(LOGIN_FAILED_P);
                _ServerSender.Flush();
                return;
            }
            Log("Client successfully connected.");
            Package LOGIN_SUCCESS_P = new Package(Command.LOGIN_SUCCESS, "");
            _ServerSender.Send(LOGIN_SUCCESS_P);
            _ServerSender.Flush();
            _Socket.setSoTimeout(Timeout);
            while (true)
            {
                Package MESSAGE = (Package)_ServerListener.Get();
                switch(MESSAGE._Command)
                {
                    case MESSAGE:
                        Log("Message from client       : " + MESSAGE._Data);
                        MESSAGE._Data = "#" + MESSAGE._Data;
                        Log("Sending message to client : " + MESSAGE._Data);
                        _ServerSender.Send(MESSAGE);
                        _ServerSender.Flush();
                        break;
                    default:
                        break;
                }

            }
        }
        catch (Exception e) { e.printStackTrace();Disconnect(); }
    }
    public void Disconnect()
    {
        Log("Client disconnected.");
        try
        {
            _Socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        Fail();
    }
    private void Start()
    {
        _ServerListener = new ServerListener(_Server, _Socket);
        Thread _ServerListenerThread = new Thread(_ServerListener);
        _ServerListenerThread.start();
        _ServerSender = new ServerSender(_Server, _Socket);
        Thread _ServerSenderThread = new Thread(_ServerSender);
        _ServerSenderThread.start();
        while(!_ServerListener.Started()||!_ServerSender.Started());
        Accept();
        if(!_Failed) _Server.Add(_ServerSender, _ServerListener, _ServerSenderThread, _ServerListenerThread);
        _Started = true;
    }
    public void run() { Start(); }
}

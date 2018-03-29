package com.lzh.testwb;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzh.testwb.socket.SocketTransceiver;
import com.lzh.testwb.socket.TcpClient;
import com.lzh.testwb.util.UtilThread;
import com.lzh.whiteboardlib.SketchView;
import com.lzh.whiteboardlib.TransUtils;
import com.lzh.whiteboardlib.WhiteBoardCmd;
import com.lzh.whiteboardlib.bean.StrokeRecord;
import com.lzh.whiteboardlib.utils.MLog;

import java.util.List;

/**
 * Created by liuzhenhui on 2018/3/13.
 */

public class SocketSketchActivity extends AppCompatActivity  {
    public static final int PORT = 1234;

    private Button bnConnect;
    private EditText edIP;

    private WhiteBoardFragment whiteBoardFragment;
    private SketchView mSketchView;
    private TcpClient client = new TcpClient() {
        @Override
        public void onConnect(SocketTransceiver transceiver) {
            refreshUI(true);
        }

        @Override
        public void onDisconnect(SocketTransceiver transceiver) {
            refreshUI(false);
        }

        @Override
        public void onConnectFailed() {
            UtilThread.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SocketSketchActivity.this, "连接失败",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onReceive(final SocketTransceiver transceiver, final String s) {
            UtilThread.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MLog.d(MLog.TAG_SOCKET,"SocketSketchActivity->run " + s);
                    WhiteBoardCmd wbStroke = JSON.parseObject(s, WhiteBoardCmd.class);
                        switch (wbStroke.getCmd()) {
                            case WhiteBoardCmd.CMD_CLEAR:
                                mSketchView.erase(false);
                                break;
                            case WhiteBoardCmd.CMD_DRAW:
                                StrokeRecord strokeRecord = TransUtils.resumeStrokeRecord(wbStroke);
                                mSketchView.addRecord(strokeRecord);
                                break;
                            case WhiteBoardCmd.CMD_DELETE:
                                mSketchView.deleteRecord(wbStroke.getUid(), wbStroke.getSq(),false);
                                break;
                        }

                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket);

        bnConnect = (Button) this.findViewById(R.id.bn_connect);
        bnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        edIP = (EditText) this.findViewById(R.id.ed_ip);

        refreshUI(false);
        FragmentTransaction ts = getSupportFragmentManager().beginTransaction();
        whiteBoardFragment = WhiteBoardFragment.newInstance();
        ts.add(R.id.fl_main, whiteBoardFragment, "wb").commit();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSketchView = whiteBoardFragment.getSketchView();
        mSketchView.setOnStrokeRecordChangeListener(new SketchView.OnStrokeRecordChangeListener() {
            @Override
            public void onPathDrawFinish(final StrokeRecord strokeRecord) {
                UtilThread.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (client.isConnected()) {
                            List<String> strokeRecordStrings = TransUtils.transStrokeRecord(strokeRecord);
                            for (String strokeRecordString : strokeRecordStrings) {
                                client.getTransceiver().send(strokeRecordString);
                            }
                        }
                    }
                });
            }

            @Override
            public void onPathDeleted(final long userid, final int sq) {
                UtilThread.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (client.isConnected()) {
                            WhiteBoardCmd cmd = new WhiteBoardCmd();
                            cmd.setCmd(WhiteBoardCmd.CMD_DELETE);
                            cmd.setUid(userid);
                            cmd.setSq(sq);
                            String cmdStr = JSONObject.toJSONString(cmd);
                            client.getTransceiver().send(cmdStr);
                        }
                    }
                });
            }

            @Override
            public void onPathCleared() {
                UtilThread.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (client.isConnected()) {
                            WhiteBoardCmd cmd = new WhiteBoardCmd();
                            cmd.setCmd(WhiteBoardCmd.CMD_CLEAR);
                            String cmdStr = JSONObject.toJSONString(cmd);
                            client.getTransceiver().send(cmdStr);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onStop() {
        client.disconnect();
        super.onStop();
    }

    /**
     * 刷新界面显示
     *
     * @param isConnected
     */
    private void refreshUI(final boolean isConnected) {
        UtilThread.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                edIP.setEnabled(!isConnected);
                bnConnect.setText(isConnected ? "断开" : "连接");
            }
        });
    }

    /**
     * 设置IP和端口地址,连接或断开
     */
    private void connect() {
        if (client.isConnected()) {
            // 断开连接
            client.disconnect();
        } else {
            try {
                String hostIP = edIP.getText().toString();
                client.connect(hostIP, PORT);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

}
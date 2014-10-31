package cn.ngds.im.demo.view.chat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import cn.ngds.im.demo.R;
import cn.ngds.im.demo.domain.NgdsMessage;
import cn.ngds.im.demo.view.adapter.SimpleListAdapter;
import com.gameservice.sdk.im.IMService;
import cn.ngds.im.demo.util.DataUtils;

import java.util.List;

/**
 * MessageAdapter
 * Description:
 */
public class MessageAdapter extends SimpleListAdapter<NgdsMessage> {
    private static final int MESSAGE_TYPE_RECV_TXT = 0;
    private static final int MESSAGE_TYPE_SENT_TXT = 1;


    public MessageAdapter(Context context, List<NgdsMessage> data) {
        super(context, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int itemViewType = getItemViewType(position);
        NgdsMessage ngdsLastMsg = position >= 1 ? getItem(position - 1) : null;
        if (MESSAGE_TYPE_SENT_TXT == itemViewType) {
            ViewHolder sendMsgItemViewHolder;
            if (null == convertView) {
                convertView = mInflater.inflate(R.layout.item_chat_sent_message, null);
                sendMsgItemViewHolder = new ViewHolder(convertView);
                convertView.setTag(sendMsgItemViewHolder);
            } else {
                sendMsgItemViewHolder = (ViewHolder) convertView.getTag();
            }
            sendMsgItemViewHolder.setContent(getItem(position), ngdsLastMsg);
        } else if (MESSAGE_TYPE_RECV_TXT == itemViewType) {
            ViewHolder receiveMsgItemViewHolder;
            if (null == convertView) {
                convertView = mInflater.inflate(R.layout.item_chat_received_message, null);
                receiveMsgItemViewHolder = new ViewHolder(convertView);
                convertView.setTag(receiveMsgItemViewHolder);
            } else {
                receiveMsgItemViewHolder = (ViewHolder) convertView.getTag();
            }
            receiveMsgItemViewHolder.setContent(getItem(position), ngdsLastMsg);
        }

        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }


    @Override
    public int getItemViewType(int position) {
        NgdsMessage message = getItem(position);
        if (message.isDirectSend()) {
            return MESSAGE_TYPE_SENT_TXT;
        } else {
            return MESSAGE_TYPE_RECV_TXT;
        }
    }

    class ViewHolder {
        private TextView tvContent;
        private TextView tvTimeStamp;
        private TextView tvDelivered;
        private ProgressBar progressBar;
        private ImageView ivFailureStatus;

        public ViewHolder(View view) {
            tvContent = (TextView) view.findViewById(R.id.tv_chatcontent);
            tvTimeStamp = (TextView) view.findViewById(R.id.tv_timestamp);
            tvDelivered = (TextView) view.findViewById(R.id.tv_delivered);
            progressBar = (ProgressBar) view.findViewById(R.id.pb_sending);
            ivFailureStatus = (ImageView) view.findViewById(R.id.iv_status);

        }

        public void setContent(final NgdsMessage item, NgdsMessage mLastItem) {
            //设置消息时间
            if (mLastItem == null || !DataUtils.isCloseEnough(mLastItem.time, item.time)) {
                tvTimeStamp.setVisibility(View.VISIBLE);
                tvTimeStamp.setText(DataUtils.getTimestampString(item.time));
            } else {
                tvTimeStamp.setVisibility(View.GONE);
            }
            //设置消息内容
            tvContent.setText(item.mIMMessage.content);
            //设置发送进度条
            if (null != progressBar && item.isDirectSend() && item.serverReceived) {
                progressBar.setVisibility(View.GONE);
            }
            //默认隐藏发送失败图片
            if (null != ivFailureStatus) {
                ivFailureStatus.setVisibility(View.GONE);
            }
            //设置发送失败时重发监听
            if (null != ivFailureStatus && item.isDirectSend() && item.sendFailure) {
                ivFailureStatus.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                ivFailureStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        item.sendFailure = false;
                        IMService.getInstance().sendPeerMessage(item.mIMMessage);
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
            }
            //是否显示送达状态
            if (null != tvDelivered) {
                if (item.receiverReceived) {
                    tvDelivered.setVisibility(View.VISIBLE);
                } else {
                    tvDelivered.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * 服务端收到信息反馈
     *
     * @param localMsgId
     */
    public void onServerAck(int localMsgId) {
        List<NgdsMessage> ngdsMessageList = getData();
        for (int i = 0; i < ngdsMessageList.size(); i++) {
            NgdsMessage message = ngdsMessageList.get(i);
            if (message.isDirectSend() && message.mIMMessage.msgLocalID == localMsgId) {
                message.serverReceived = true;
                notifyDataSetChanged();
                return;
            }
        }
    }

    /**
     * 接收方收到信息反馈
     *
     * @param localMsgId
     */
    public void onReceiverAck(int localMsgId) {
        List<NgdsMessage> ngdsMessageList = getData();
        for (int i = 0; i < ngdsMessageList.size(); i++) {
            NgdsMessage message = ngdsMessageList.get(i);
            if (message.isDirectSend() && message.mIMMessage.msgLocalID == localMsgId) {
                message.receiverReceived = true;
                notifyDataSetChanged();
                return;
            }
        }
    }

    /**
     * 消息发送失败反馈
     *
     * @param localMsgId
     * @param uid
     */
    public void onSendFail(int localMsgId, long uid) {
        List<NgdsMessage> ngdsMessageList = getData();
        for (int i = 0; i < ngdsMessageList.size(); i++) {
            NgdsMessage message = ngdsMessageList.get(i);
            if (message.isDirectSend() && message.mIMMessage.msgLocalID == localMsgId) {
                message.sendFailure = true;
                notifyDataSetChanged();
                return;
            }
        }
    }

}
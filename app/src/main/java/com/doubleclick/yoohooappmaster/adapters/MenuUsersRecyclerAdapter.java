package com.doubleclick.yoohooappmaster.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.doubleclick.yoohooappmaster.R;
import com.doubleclick.yoohooappmaster.interfaces.ChatItemClickListener;
import com.doubleclick.yoohooappmaster.models.Chat;
import com.doubleclick.yoohooappmaster.models.User;
import com.doubleclick.yoohooappmaster.utils.Helper;

import java.util.ArrayList;

/**
 * Created by mayank on 7/5/17.
 */

public class MenuUsersRecyclerAdapter extends RecyclerView.Adapter<MenuUsersRecyclerAdapter.BaseViewHolder> implements Filterable {
    private Context context;
    private ChatItemClickListener itemClickListener;
    private ArrayList<User> dataList, dataListFiltered;
    private Filter filter;
    private User userMe;

    public MenuUsersRecyclerAdapter(@NonNull Context context, @Nullable ArrayList<User> users) {
        if (context instanceof ChatItemClickListener) {
            this.itemClickListener = (ChatItemClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ChatItemClickListener");
        }

        this.context = context;
        this.userMe = new Helper(context).getLoggedInUser();
        this.dataList = users;
        this.dataListFiltered = users;
        this.filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    dataListFiltered = dataList;
                } else {
                    ArrayList<User> filteredList = new ArrayList<>();
                    for (User row : dataList) {
                        String toCheckWith = TextUtils.isEmpty(row.getName()) ? row.getId() : row.getName();
                        if (toCheckWith.toLowerCase().startsWith(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }
                    dataListFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = dataListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                dataListFiltered = (ArrayList<User>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public int getItemViewType(int position) {
        return dataListFiltered.get(position).getId().equals("-1") && dataListFiltered.get(position).getName().equals("-1") ? 0 : 1;
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1)
            return new UsersViewHolder(LayoutInflater.from(context).inflate(R.layout.item_menu_user, parent, false));
        else
            return new UserInviteHeaderViewHolder(LayoutInflater.from(context).inflate(R.layout.item_menu_user_invite, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        if (holder instanceof UsersViewHolder) {
            ((UsersViewHolder) holder).setData(dataListFiltered.get(position));
        } else if (holder instanceof UserInviteHeaderViewHolder) {
            ((UserInviteHeaderViewHolder) holder).setData(dataListFiltered.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return dataListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return this.filter;
    }

    class BaseViewHolder extends RecyclerView.ViewHolder {
        public BaseViewHolder(View itemView) {
            super(itemView);
        }
    }

    class UserInviteHeaderViewHolder extends BaseViewHolder {
        private TextView inviteTitle;

        UserInviteHeaderViewHolder(final View itemView) {
            super(itemView);
            inviteTitle = itemView.findViewById(R.id.inviteTitle);
        }

        public void setData(User user) {
            inviteTitle.setText(context.getString(R.string.invite_to) + " " + context.getString(R.string.app_name));
        }
    }

    class UsersViewHolder extends BaseViewHolder {
        private ImageView userImage;
        private TextView userName, inviteText;

        UsersViewHolder(final View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            inviteText = itemView.findViewById(R.id.inviteText);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != -1) {
                    if (dataList.get(pos).isInviteAble()) {
//                        Intent it = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + dataListFiltered.get(pos).getId()));
//                        it.putExtra("sms_body", String.format(context.getString(R.string.invitation_message), context.getString(R.string.app_name), context.getPackageName()));
//                        context.startActivity(it);
                        Helper.openShareIntent(context, null, (context.getString(R.string.hey_there) + " " + context.getString(R.string.app_name) + "\n" + context.getString(R.string.download_now) + ": " + ("https://play.google.com/store/apps/details?id=" + context.getPackageName())));
                    } else {
                        itemClickListener.onChatItemClick(new Chat(dataListFiltered.get(pos), userMe.getId()), pos, userImage);
                    }
                }
            });
        }

        public void setData(User user) {
            userName.setText(user.getName());
            inviteText.setVisibility(user.isInviteAble() ? View.VISIBLE : View.GONE);
            Glide.with(context).load(user.getImage()).apply(new RequestOptions().placeholder(R.drawable.yoohoo_placeholder)).into(userImage);
            userName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }
}

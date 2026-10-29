/*
 * Copyright 2026 Noven (tontonvs)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noven.rberry.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.playsoftware.j2meloader.R;

/**
 * The first cards on the home grid: the built-in games, then the "+" card that opens the import
 * picker. Imported games follow in the other adapter. Both sit in one grid through a
 * ConcatAdapter, so the upstream list code stays untouched.
 */
public class HomeCardsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onBuiltInGameClicked(@NonNull BuiltInGame game);

        void onAddGameClicked();
    }

    private static final int TYPE_BUILT_IN = 0;
    private static final int TYPE_ADD = 1;

    private final Context context;
    private final Listener listener;
    private final List<BuiltInGame> allGames = BuiltInGames.all();
    private final List<BuiltInGame> shownGames = new ArrayList<>(allGames);
    private boolean showAddCard = true;

    public HomeCardsAdapter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    /** Filters built-in games by title. The "+" card is hidden while a search is active. */
    @SuppressLint("NotifyDataSetChanged")
    public void setQuery(CharSequence query) {
        String q = query == null ? "" : query.toString().trim().toLowerCase(Locale.ROOT);
        shownGames.clear();
        for (BuiltInGame game : allGames) {
            if (q.isEmpty() || context.getString(game.titleRes).toLowerCase(Locale.ROOT).contains(q)) {
                shownGames.add(game);
            }
        }
        showAddCard = q.isEmpty();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return shownGames.size() + (showAddCard ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        return position < shownGames.size() ? TYPE_BUILT_IN : TYPE_ADD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ADD) {
            View view = inflater.inflate(R.layout.item_card_add, parent, false);
            view.setOnClickListener(v -> listener.onAddGameClicked());
            return new AddHolder(view);
        }
        return new BuiltInHolder(inflater.inflate(R.layout.item_card_builtin, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BuiltInHolder) {
            ((BuiltInHolder) holder).bind(shownGames.get(position));
        }
    }

    private static final class AddHolder extends RecyclerView.ViewHolder {
        AddHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private final class BuiltInHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;

        BuiltInHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.card_icon);
            title = itemView.findViewById(R.id.card_title);
        }

        void bind(BuiltInGame game) {
            icon.setImageResource(game.iconRes);
            title.setText(game.titleRes);
            itemView.setOnClickListener(v -> listener.onBuiltInGameClicked(game));
        }
    }
}

package pl.borewicz.graphmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

public class MovieActivity extends AppCompatActivity {

    //private Map<String, String> movieInfo;
    private List<String[]> movieInfo;
    String imageUrl, trailer, title;
    ArrayAdapter<String[]> adapter;
    RecyclerView recyclerView;
    Toolbar toolbar;
    CollapsingToolbarLayout collapsingToolbarLayout;
    Map<String, String> relationsMapping = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        movieInfo = new ArrayList<String[]>();

        setContentView(R.layout.activity_movie);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        //collapsingToolbarLayout.setTitleEnabled(false);

        recyclerView = (RecyclerView) findViewById(R.id.att_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        relationsMapping.put("DIRECTED", "Directed by");
        relationsMapping.put("ACTS_IN", "Cast");

        new LoadMovie(getIntent().getIntExtra("movie_id", 0)).execute();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!trailer.isEmpty()) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(trailer));
                    startActivity(i);
                }
                else
                    Snackbar.make(view, "Trailer not available", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
            }
        });
    }

    public class MovieAdapter extends RecyclerView.Adapter {
        private RecyclerView mRecyclerView;
        private List<String[]> mDataset;

        private class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView mTitle;
            public TextView mContent;

            public MyViewHolder(View pItem) {
                super(pItem);
                mTitle = (TextView) pItem.findViewById(R.id.article_title);
                mContent = (TextView) pItem.findViewById(R.id.article_subtitle);
            }
        }

        public MovieAdapter(List<String[]> pDataset, RecyclerView pRecyclerView) {
            mDataset = pDataset;
            mRecyclerView = pRecyclerView;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.movie_card, viewGroup, false);

            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int i) {
            String[] entry = mDataset.get(i);
            ((MyViewHolder) viewHolder).mTitle.setText(entry[0]);
            ((MyViewHolder) viewHolder).mContent.setText(entry[1]);
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }

    class LoadMovie extends AsyncTask<String, String, String> {
        String query;
        JsonObject json;
        int mMovieId;

        LoadMovie(int movieId) {
            mMovieId = movieId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... args) {
            try {
                query = String.format(Locale.US, "MATCH (m:Movie) WHERE id(m)=%d return m", mMovieId);
                json = Neo4jDriver.sendPost(query);

                JsonObject data = json.getJsonArray("data").getJsonArray(0).getJsonObject(0).getJsonObject("data");
                    //movieInfo.add(new String[] { "Title", node.get("title").asString()});
                if (data.containsKey("tagline") || !data.getString("tagline").equals(""))
                    movieInfo.add(new String[]{"Tagline", data.getString("tagline")});
                if (data.containsKey("studio"))
                    movieInfo.add(new String[]{"Studio", data.getString("studio")});
                movieInfo.add(new String[]{"Description", data.getString("description")});
                movieInfo.add(new String[]{"Language", data.getString("language")});
                movieInfo.add(new String[]{"Genre", data.getString("genre")});

                if (data.containsKey("trailer"))
                    trailer = data.getString("trailer");
                if (data.containsKey("imageUrl"))
                    imageUrl = data.getString("imageUrl");
                title = data.getString("title");

                query = String.format(Locale.US, "MATCH (m)-[r]->(n:Movie) WHERE id(n)=%d return type(r) as rel_type,collect(m.name) as list", mMovieId);
                json = Neo4jDriver.sendPost(query);

                for (JsonArray arr : json.getJsonArray("data").getValuesAs(JsonArray.class))
                {
                    String rel_type = arr.getString(0);
                    ArrayList<String> list = new ArrayList<String>();
                    for (JsonString op : arr.getJsonArray(1).getValuesAs(JsonString.class))
                        list.add(op.getString());
                    movieInfo.add(new String[]{relationsMapping.get(rel_type),
                            TextUtils.join(", ", list)});
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String file_url) {
            //pDialog.dismiss();
            runOnUiThread(new Runnable() {
                public void run() {
                    recyclerView.setAdapter(new MovieAdapter(movieInfo, recyclerView));
                    //collapsingToolbarLayout.setTitleEnabled(false);
                    collapsingToolbarLayout.setTitle(title);
                }
            });
        }
    }
}

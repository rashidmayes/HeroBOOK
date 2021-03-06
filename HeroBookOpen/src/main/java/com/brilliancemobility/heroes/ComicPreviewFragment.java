package com.brilliancemobility.heroes;

import java.util.ArrayList;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.brilliancemobility.heroes.net.Comic;
import com.brilliancemobility.heroes.net.MarvelCharacter;
import com.brilliancemobility.heroes.net.MarvelData;
import com.brilliancemobility.heroes.net.MarvelResponse;
import com.brilliancemobility.heroes.net.REST;
import com.brilliancemobility.heroes.net.Thumbnail;
import com.brilliancemobility.heroes.net.VolleySingleton;
import com.brilliancemobility.heroes.util.AnalyticsUtils;
import com.brilliancemobility.heroes.util.ScrollChecker;
import com.ortiz.touch.TouchImageView;
import com.google.gson.Gson;

public class ComicPreviewFragment extends Fragment implements OnItemClickListener, OnLongClickListener, OnClickListener, ScrollChecker
{
	protected Comic mCurrent;	
	private TouchImageView mPreview;	
	
	private View mContentView;
	private int mPosition;
	private Bitmap mThumbnail;
	private Gson gson = new Gson();
	private View mDetails;
	private TextView mTitle;
	private TextView mDescription;
	private TextView mAlias;
	
	private ImageView mFullscreen;
	private GridView mGallery;
	private ArrayAdapter<MarvelCharacter> mGalleryAdapter;

	private MyCallBack mCallback;
	private int mPageSize = 10;
	
    @Override
    public void onActivityCreated(Bundle savedInstanceState) 
    {
        super.onActivityCreated(savedInstanceState);         
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(false);
        this.setHasOptionsMenu(false);
        
        Bundle args = getArguments();
        String json = args.getString("comic");
        mCurrent = gson.fromJson(json, Comic.class);
	    
        mPosition = args.getInt("position");
        
        mGalleryAdapter = new GalleryAdapter();
        
        mCallback = new MyCallBack();
        REST.getMarvel(getActivity()).comicCharacters(mCurrent.id,0, mPageSize, "name", mCallback);
        
        AnalyticsUtils.trackComicViews(getActivity(), mCurrent.id, mCurrent.name, 1);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
    {
    	mContentView = inflater.inflate(R.layout.fragment_view_character, null);
    	mContentView.setTag(R.id.preview,mPosition);
 	
		
        mPreview = (TouchImageView) mContentView.findViewById(R.id.preview);
        mPreview.setOnClickListener(this);
        mPreview.setOnLongClickListener(this);
        
        mPreview.setImageBitmap(mThumbnail);
        mPreview.setImageUrl(mCurrent.thumbnail.getPicUrl(),VolleySingleton.getInstance(getActivity()).getImageLoader());

        //TextView textView = (TextView)mContentView.findViewById(R.id.title);
        //textView.setText(mCurrent.name);
        
        //textView = (TextView)mContentView.findViewById(R.id.description);
        //textView.setText(mCurrent.description);
    	
        mDetails = mContentView.findViewById(R.id.details);
        mDescription = (TextView)mContentView.findViewById(R.id.description);
        mDescription.setMovementMethod(LinkMovementMethod.getInstance());
        mTitle = (TextView)mContentView.findViewById(R.id.title);
        mAlias = (TextView)mContentView.findViewById(R.id.alias);
        mAlias.setOnClickListener(this);
        
        mFullscreen = (ImageView)mContentView.findViewById(R.id.fullscreen);
        mFullscreen.setOnClickListener(this);
        
        mContentView.findViewById(R.id.share).setOnClickListener(this);        
        
        mTitle.setText(mCurrent.title);
        mDescription.setText(Html.fromHtml(mCurrent.getLongDescription(getActivity())));
        if ( "0".equals(mCurrent.issueNumber) )
        {
        	mAlias.setText("ID #" + mCurrent.id);
        }
        else
        {
        	mAlias.setText("ISSUE #" + mCurrent.issueNumber);
        }

        mGallery = (GridView)mContentView.findViewById(R.id.gallery);
        mGallery.setOnItemClickListener(this);
        mGallery.setAdapter(mGalleryAdapter);

        return mContentView;
    }
    
    
    class MyCallBack implements Callback<MarvelResponse<MarvelCharacter>>
    {
    	@Override
    	public void failure(RetrofitError error)
    	{
    		if ( mCallback == this )
    		{
	    		//handle error
    		}
    	}


    	@Override
    	public void success(MarvelResponse<MarvelCharacter> marvelResponse, Response response)
    	{
    		if ( mCallback == this )
    		{
        		MarvelData<MarvelCharacter> data = marvelResponse.data;
        		if ( data.count > 0 )
        		{
        			mGallery.setVisibility(View.VISIBLE);
        		}
        		
        		for ( MarvelCharacter mc : data.results )
        		{
        			if ( (mc.thumbnail != null) ) 
        			{
        				mGalleryAdapter.add(mc);
        			}
        		}
    		}
    	}
    }    
    
    
    
    @Override
	public void onItemClick(AdapterView<?> a, View view, int position, long id)
	{
    	if ( a == mGallery )
    	{
    		MarvelCharacter item = mGalleryAdapter.getItem(position);
    		Tools.showCharacter(getActivity(), null, mGalleryAdapter, mCurrent, item, position, 0, 10, null);
    	}
	}
    
	@Override
	public boolean shouldScroll(MotionEvent event)
	{
		return !(mPreview != null && mPreview.canMove(event));
	}
	
	
	public void toggleDetails()
	{
		if ( mDetails.getVisibility() == View.VISIBLE )
		{
			mDetails.setVisibility(View.GONE);
			mFullscreen.setImageResource(R.drawable.ic_action_return_from_full_screen);
		}
		else
		{
			mDetails.setVisibility(View.VISIBLE);
			mFullscreen.setImageResource(R.drawable.ic_action_full_screen);
		}
	}
	
	
	public void openUrl(String url)
	{
		try
		{
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(browserIntent);
		}
		catch (Exception e)
		{
			Tools.showErrorMessage(getActivity(), e.getLocalizedMessage());
		}
	}
    	
	
	@Override
	public boolean onLongClick(View view)
	{
		if ( view.getId() == R.id.preview )
		{
			//Session.download(this.getActivity(),null,mCurrent);
		}
		
		return true;
	}

	public void onClick(View view)
	{
		if ( view.getId() == R.id.preview || view.getId() == R.id.fullscreen )
		{
			toggleDetails();
		}
		else if ( view.getId() == R.id.share )
		{
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, mCurrent.getLink());
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, mCurrent.name);
            startActivity(Intent.createChooser(intent, getString(R.string.share)));
		}
		else if ( view.getId() == R.id.alias )
		{
			//Tools.toggleBookmark(getActivity(), mHandler, mCurrent);
		}
	}


	public void setThumbnail(Bitmap thumbnail)
	{
		this.mThumbnail = thumbnail;
	}
	
	
	class GalleryAdapter extends ArrayAdapter<MarvelCharacter>
	{
		LayoutInflater layoutInflater;
		Resources resources;
		Locale locale;
		
		public GalleryAdapter()
		{
			super(getActivity(), R.layout.cell_comic, new ArrayList<MarvelCharacter>());
			
			layoutInflater = LayoutInflater.from(getActivity());
			resources = getResources();
			locale = Locale.getDefault();
		}
		
		
	    private class ViewHolder
	    {
	    	NetworkImageView imageView;
	    	TextView title; 
	    }
	    
	    public View getView(int position, View convertView, ViewGroup parent) 
	    {
	    	ViewHolder holder;
	    	if (convertView == null) 
	    	{
	    		convertView = layoutInflater.inflate(R.layout.cell_comic, null);
		    	
	    		holder = new ViewHolder();
	    	    holder.imageView = (NetworkImageView) convertView.findViewById(R.id.icon);
	    	    holder.title = (TextView)convertView.findViewById(R.id.title);
	    	    
	    	    convertView.setTag(holder);
	    	}
	    	else 
	    	{
	    	    holder = (ViewHolder) convertView.getTag();
	    	}
	    	
	        try
	        {
	        	MarvelCharacter record = getItem(position);

	        	holder.title.setText(record.getAlias());
	        	holder.imageView.setImageUrl(record.thumbnail.getPicUrl(Thumbnail.SIZE.portrait_small),VolleySingleton.getInstance(getActivity()).getImageLoader());
	        }
	        catch (Exception e)
	        {
	        	Log.e("null", "null", e);
	        }
	        
	        return convertView;
	    }
		
	}
	

}

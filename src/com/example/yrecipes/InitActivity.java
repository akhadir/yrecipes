package com.example.yrecipes;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;


import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
//import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.TextView;


@SuppressLint("SetJavaScriptEnabled")
public class InitActivity extends Activity {
    
	public TextView tv;
	public int currentLayout;
	public Hashtable <Integer,Object> searchData = new Hashtable<Integer,Object>();
	public Hashtable <String,Object> filters = new Hashtable<String,Object>();
	public LinearLayout contentLayout;
	public ListView settingsView;
	public SelfSearchSource src;
	public Hashtable<String,View> filterLayouts;
	public Hashtable<String,ArrayList<String>> filterStrings = new Hashtable<String,ArrayList<String>>();
	public static String filename = "MySharedString";
	public String currentSearchKeyword;
	public static ArrayList<ImageView> searchImageViews = new ArrayList<ImageView>();
	public Timer timer;
	static Boolean updateImages = true;
	public Boolean loadingContent = false;
	PopupWindow loader;
	SharedPreferences prefData;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        src = new SelfSearchSource();
        currentLayout = R.layout.activity_init;
        prefData = getSharedPreferences(filename, 0);
        String dataReturn = prefData.getString("dishPref", "");
    	if (dataReturn.equals("")) {
    		this.setSelectScreen();
    	} else {
    		this.setRegularScreen(dataReturn);
    	}
    }	
    void setRegularScreen(String keyword) {
    	this.currentSearchKeyword = keyword.trim();
    	setContentView(currentLayout);
    	this.showSpinner();
    	new SourceAsync().execute();
    }
    void switchToWebVIew(View v) {
		currentLayout = R.layout.web;
    	setContentView(currentLayout);
		WebView wv = (WebView) findViewById(R.id.webView1);
		String clickURL = v.getTag().toString();
		wv.loadUrl(clickURL);
		wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new InsideWebViewClient());
		this.showSpinner();
    }
    
    public class SourceAsync extends AsyncTask<String, Integer, String>{

		@Override
		protected String doInBackground(String... params) {
			if (!loadingContent) {
				loadingContent = true;
				try {
					src.getSource(currentSearchKeyword, 1, filterStrings);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		protected void onPostExecute(String result){
			loader.dismiss();
			searchData = src.getRecipeData();
			if (searchData != null && searchData.size() > 0) {
				filters = src.getFilterData();
				SharedPreferences.Editor editor = prefData.edit();
				JSONArray js = new JSONArray();
				js.put(searchData);
				editor.putString("searchData",js.toString() );
				editor.commit();
				setDefaultLayout();
				loadingContent = false;
			} else {
				Toast.makeText(getApplicationContext(), "Error Loading Content. Try again later.", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}
    public static class ImageLoader extends AsyncTask<ArrayList<ImageView>, Void, ArrayList<ImageView>>{
    	static Hashtable<String,Drawable> downloadedImages = new Hashtable<String,Drawable>();
		@Override
		protected ArrayList<ImageView> doInBackground(ArrayList<ImageView>... params) {
			ArrayList<ImageView> input = params[0];
			int len = input.size();
			Drawable drawable;
			String img;
			for (int i = 0; i < len; i++) {
				ImageView in = input.get(i);
				img = in.getTag().toString();
				if (downloadedImages.containsKey(img)) {
					drawable = downloadedImages.get(img);
				} else {
					try {
						drawable = LoadImageFromWebOperations(img);
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
					downloadedImages.put(img, drawable);
				}
			}
			return input;
		}

		protected void onPostExecute(ArrayList<ImageView> in){
			if (in != null) {
				setImageViews();
			}
		}
	}

    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	setImageViews();
        }
    };
    public class imageUpdater extends TimerTask {
    	public void run(){
    		if (searchImageViews != null && searchImageViews.size() > 0) {
    			handler.sendEmptyMessage(0);
    		} else {
    			timer.cancel();
    		}
    	}
    	
    }
    public static void setImageViews() {
    	if (updateImages) {
    		ArrayList<ImageView> unUpdatedIvs = new ArrayList<ImageView>();
    		updateImages = false;
	    	int len = searchImageViews.size();
	    	for (int i = 0; i < len; i++) {
	    		ImageView iv = searchImageViews.get(i);
	    		if (iv != null) {
		    		String img = iv.getTag().toString();
		    		if (ImageLoader.downloadedImages.containsKey(img)) {
		    			Drawable d = ImageLoader.downloadedImages.get(img);
		    			iv.setImageDrawable(d);
		    		} else {
		    			unUpdatedIvs.add(iv);
		    		}
	    		}
	    	}
	    	searchImageViews = unUpdatedIvs;
	    	updateImages = true;
    	}
    }
    void setSearchEntity(LinearLayout l, String Src, String text1, String text2, String clickURL) {
    	final View inflate = getLayoutInflater().inflate(R.layout.sresults_temp, null);
    	ImageView inew = (ImageView) inflate.findViewById(R.id.imageView1);
    	inew.setTag(Src);
    	searchImageViews.add(inew);
        //inew.loadUrl(Src);
        TextView tv = (TextView) inflate.findViewById(R.id.textView1);
        tv.setText(text1);
        tv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switchToWebVIew(inflate);
			}
		});
        tv.setSelected(true);
        
        tv = (TextView)	inflate.findViewById(R.id.textView2);
        tv.setText(text2);
        tv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switchToWebVIew(inflate);
			}
		});
        l.addView(inflate);
        inflate.setClickable(false);
        inflate.setFocusable(true);
        inflate.setTag(clickURL);
        inew.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int t = event.getAction();
				if (t != MotionEvent.ACTION_MOVE && 
						t != MotionEvent.ACTION_DOWN &&
						t != MotionEvent.ACTION_OUTSIDE &&
						t != MotionEvent.ACTION_CANCEL &&
						t == MotionEvent.ACTION_UP) {
					
					switchToWebVIew(inflate);
				}
				return false;
			}
		});
    }
    @SuppressWarnings("unchecked")
	void setDefaultLayout() {
    	searchImageViews.clear();
    	Display display = getWindowManager().getDefaultDisplay();
    	
    	int width = display.getWidth();
    	int numPanels = 2;//((height - 300) / 150);
    	if (numPanels <= 0) {
    		numPanels = 1;
    	}
    	int minItemPerPanel = width / 220;
		LinearLayout P = (LinearLayout) findViewById(R.id.hslinearlayoutMain);
        P.setOrientation(LinearLayout.VERTICAL);
        
        //S.setWeightSum(50);
        LinearLayout A = new LinearLayout(this);
        A.setOrientation(LinearLayout.HORIZONTAL);
        
        P.addView(A);
       
        String image, title, duration, clickURL;
        int e = this.searchData.size();
        LinearLayout s = new LinearLayout(this), l[] = new LinearLayout[100];
        int k = 0;
        Boolean initLflag = false;
        Hashtable<String,String> h;
        for (int i = 0; i < e; i++) {
        	if ((i % minItemPerPanel) == 0) {
        		if (k >= numPanels) {
        			k = 0;
        			s = l[0];
        			initLflag = true;
        			minItemPerPanel = 1;
        		} else { 
        			if (!initLflag) {
        				l[k] = new LinearLayout(this);
        				l[k].setOrientation(LinearLayout.HORIZONTAL);
        				P.addView(l[k]);
        			}
	                s = l[k];
        		}
        		k++;
        	}
        	h = (Hashtable<String,String>) this.searchData.get(i);
        	image = h.get("image");
        	title = h.get("title");
        	duration = h.get("duration");
        	clickURL = h.get("URL");
        	setSearchEntity(s, image, title, duration, clickURL);
        }
        new ImageLoader().execute(searchImageViews);
        timer = new Timer();
        timer.schedule(new imageUpdater(), 500, 500);
        tv = (TextView) findViewById(R.id.autoCompleteTextView1);
        tv.setText(this.currentSearchKeyword);
        tv.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String t = tv.getText().toString();
				String defVal =  getResources().getString(R.string.search_ghost_text);
				if (t.equals(defVal)) {
					tv.setText("");
					//InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					// only will trigger it if no physical keyboard is open
					//mgr.showSoftInput(tv, InputMethodManager.SHOW_IMPLICIT);
				}
				
			}
			
		});
        tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					String t = tv.getText().toString();
					String defVal =  getResources().getString(R.string.search_ghost_text);
					if (t.equals("")) {
						tv.setText(defVal);
					}
					
					//InputMethodManager imm = (InputMethodManager)getSystemService(
					//	      Context.INPUT_METHOD_SERVICE);
					//imm.hideSoftInputFromWindow(tv.getWindowToken(), 0);
				}
			}
		});
        settingsView = (ListView) findViewById(R.id.listView1);
        String[] values = new String[] { "Saved Searches", "Reset Home", "Last 10 Searches"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        		  android.R.layout.simple_list_item_1, android.R.id.text1, values);
        ImageButton ib = (ImageButton) this.findViewById(R.id.ImageButton01);
        settingsView.setAdapter(adapter);
        ib.setTag(settingsView);
        ib.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ListView lv = (ListView) v.getTag();
				int visibility = lv.getVisibility();
				if (ListView.VISIBLE == visibility) {
					lv.setVisibility(ListView.GONE);
				} else {
					lv.setVisibility(ListView.VISIBLE);
				}
			}
		});
        
        ImageButton sb = (ImageButton) this.findViewById(R.id.imageButton1);
        sb.setTag(this);
        sb.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				InitActivity parent = InitActivity.this;
				String searchTxt = tv.getText().toString().trim();
				if (!searchTxt.equals(R.string.search_ghost_text) && !searchTxt.equals("") && 2 < searchTxt.length()) {
					parent.setNewResults(searchTxt);
				}
			}
		});
        
        ImageView filterTimer = (ImageView) this.findViewById(R.id.imageView1);
        this.setFilterPopUp(filterTimer, "totaltime", "Filter By Duration", false);
        ImageView filterIng = (ImageView) this.findViewById(R.id.imageView2);
        this.setFilterPopUp(filterIng, "ingredient_w", "Must Have Ingredients", true);
        ImageView filterIngN = (ImageView) this.findViewById(R.id.imageView3);
        this.setFilterPopUp(filterIngN, "ingredient_wo", "Not To Have Ingredients", true);
        ImageView filterMealType = (ImageView) this.findViewById(R.id.imageView4);
        this.setFilterPopUp(filterMealType, "mealtype", "Filter By Meal Type", false);
        ImageView filterDiet = (ImageView) this.findViewById(R.id.imageView5);
        this.setFilterPopUp(filterDiet, "collection", "Filter By Diet", false);
	}
    private void showSpinner() {
    	final View inflate1 = getLayoutInflater().inflate(R.layout.spinner, null);
		loader = new PopupWindow(inflate1, 250, 300, true);
		loader.setContentView(inflate1);
		inflate1.post(new Runnable() {
		   public void run() {
			   loader.showAtLocation(inflate1, Gravity.CENTER, 0, 120);
		   }
		});
    }
    private void setFilterPopUp(ImageView parent, String name, String label, Boolean multiselect) {
    	final String filterName = name;
    	final Boolean isRadioGroup = !multiselect;
    	final View inflate = getLayoutInflater().inflate(R.layout.filters, null);
    	//this.filterLayouts.put(name, inflate);
        final LinearLayout filterLayout = (LinearLayout) inflate.findViewById(R.id.filter_layout);
        filterLayout.removeAllViewsInLayout();
        TextView tv = (TextView) inflate.findViewById(R.id.textView1);
        tv.setText(label);
        CheckBox cb;
        RadioGroup cg = null;
        RadioButton cr;
        @SuppressWarnings("unchecked")
		Hashtable<String,String> filter = (Hashtable<String,String>) this.filters.get(name);
        Enumeration<String> en = null;
        if (filter != null) {
        	en = filter.keys();
        }
        String filName;
        if (!multiselect) {
        	cg = new RadioGroup(this);
        }
        ArrayList<String> filterVals = new ArrayList<String>();
        if (this.filterStrings.get(name) != null) {
    		filterVals = (ArrayList<String>) this.filterStrings.get(name);
    	}
        int idi = 0;
        while(en.hasMoreElements()) {
        	filName = en.nextElement().toString();
        	if (multiselect) {
        		cb = new CheckBox(this);
        		cb.setTag(filName);
        		cb.setText(filName);
    			if (filterVals.contains(filName)) {
    				cb.setChecked(true);
        		}
        		filterLayout.addView(cb);
        	} else {
        		cr = new RadioButton(this);
        		cr.setText(filName);
        		cr.setTag(filName);
        		cr.setId(idi++);
        		if (filterVals.contains(filName)) {
    				cr.setChecked(true);
        		}
        		cg.addView(cr);
        	}
        }
        if (!multiselect) {
        	filterLayout.addView(cg);
        }
        final PopupWindow popUp = new PopupWindow(inflate, 250, 300, true);
        popUp.setBackgroundDrawable(new BitmapDrawable());
		popUp.setContentView(inflate);
		popUp.setOutsideTouchable(true);
        parent.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (popUp.isShowing()) {
					popUp.dismiss();
				}else {
					popUp.showAtLocation(inflate, Gravity.LEFT, 71, 5);
				}
			}
		});
        popUp.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				InitActivity.this.saveFilterDataToVar(filterName, isRadioGroup, filterLayout);
			}
        });
    }
        
    private void setSelectScreen() {
    	setContentView(R.layout.yahoo_recipy);
    	ImageView[] mimage = new ImageView[10];
    	TextView[] ltext1 = new TextView[10] ;
    	LinearLayout lay11,lay12;
    	View[] inflate = new View[10];
    	String[] rec = {"Beverages","Desserts","Pizza","Meat","Cheese","Soups","Bread","Salad","",""};
    	
		lay11 = (LinearLayout) findViewById(R.id.main_lay11);
		lay12 = (LinearLayout) findViewById(R.id.main_lay12);
		
		//main code
		for(int i=0;i<10;i++){
			inflate[i]	 = getLayoutInflater().inflate(R.layout.image_lay, null);	
			inflate[i].setTag(rec[i]);
			mimage[i] = (ImageView) inflate[i].findViewById(R.id.irecipy);
			ltext1[i] = (TextView) inflate[i].findViewById(R.id.tlebel1);
			ltext1[i].setText(rec[i]);
			inflate[i].setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String stringData = v.getTag().toString();
					SharedPreferences.Editor editor = prefData.edit();
					editor.putString("dishPref", stringData);
					editor.commit();
					InitActivity.this.setRegularScreen(stringData);
				}
			});
		}
		mimage[0].setImageResource(R.drawable.beverages);
		lay11.addView(inflate[0]);
		
		mimage[1].setImageResource(R.drawable.dessert);
		lay11.addView(inflate[1]);
		
		mimage[2].setImageResource(R.drawable.pizza);
		lay12.addView(inflate[2]);
		
		mimage[3].setImageResource(R.drawable.meat);
		lay12.addView(inflate[3]);
		
		mimage[4].setImageResource(R.drawable.cheesedishes);
		lay11.addView(inflate[4]);
		
		mimage[5].setImageResource(R.drawable.soups);
		lay12.addView(inflate[5]);
		
		mimage[6].setImageResource(R.drawable.bread);
		lay12.addView(inflate[6]);
		
		mimage[7].setImageResource(R.drawable.salad);
		lay11.addView(inflate[7]);
			
    }
    private void saveFilterDataToVar(String filterName, Boolean isRadioGroup, LinearLayout v) {
    	ArrayList<String> fres = new ArrayList<String>();
    	CheckBox cb;
    	RadioButton crb;
    	RadioGroup cg;
    	int len = v.getChildCount();
    	int clen = 0;
    	Object tag, tag1;
    	for(int i=0; i<len; ++i) {
    		View nextChild = v.getChildAt(i);
    	    if (isRadioGroup) {
    	    	cg = (RadioGroup) nextChild;
    	    	clen = cg.getChildCount();
    	    	for (int j=0; j < clen; j++) {
    	    		View vc = cg.getChildAt(j);
    	    		tag1 = vc.getTag();
    	    		crb = (RadioButton) cg.findViewWithTag(tag1);
    	    		if (crb.isChecked()) {
    	    			fres.add(crb.getText().toString());
    	    			break;
    	    		}
    	    	}
    	    } else {
    	    	tag = nextChild.getTag();
    	    	cb = (CheckBox) v.findViewWithTag(tag);
    	    	if (cb.isChecked()) {
    	    		fres.add(cb.getText().toString());
    	    	}
    	    }
    	}
    	this.filterStrings.put(filterName, fres);
	}
    private void setNewResults(String keyword)  {
    	LinearLayout P = (LinearLayout) findViewById(R.id.hslinearlayoutMain);
    	P.removeAllViewsInLayout();
    	if (!this.currentSearchKeyword.equals(keyword)) {
    		this.filterStrings.clear();
    	}
    	this.setRegularScreen(keyword);
    }
    public static Drawable LoadImageFromWebOperations(String url)
    {
         try
         {
             InputStream is = (InputStream) new URL(url).getContent();
             Drawable d = Drawable.createFromStream(is, "src name");
             return d;
         }catch (Exception e) {
             System.out.println("Exc="+e);
             return null;
         }
     }
    /* Class that prevents opening the Browser */
    private class InsideWebViewClient extends WebViewClient {
    	boolean redirect = false;
    	boolean loadingFinished = true;
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String urlNewString) {
        	if (!loadingFinished) {
                redirect = true;
            }
        	loadingFinished = false;
	 	    view.loadUrl(urlNewString);
	 	    return true;
 	    }

 	    @Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
 	    	loadingFinished = false;
		}

		@Override
 	   public void onPageFinished(WebView view, String url) {
			if (!redirect){
	           loadingFinished = true;
	        }
	        if(loadingFinished && !redirect){
	    	   loader.dismiss();
	    	   JSInterface myJSInterface = new JSInterface(view);
	    	   view.addJavascriptInterface(myJSInterface, "JSInterface");
	    	   view.loadUrl("javascript:(function () { window.JSInterface.doEchoTest(document.body.innerHTML.length);})()");
	        } else{
	           redirect = false; 
	        }
	        
 	    }
    }
    
    @Override
	public void onBackPressed() {
		//super.onBackPressed();
		if (currentLayout == R.layout.web) {
		 	currentLayout = R.layout.activity_init;
		 	setContentView(currentLayout);
		 	this.setDefaultLayout();
		} else if (settingsView != null && settingsView.isShown()) {
	 		settingsView.setVisibility(ListView.GONE);
		} else {
			finish();
		}
	}
    public class JSInterface{

    	private WebView mAppView;
    	public JSInterface  (WebView appView) {
    	        this.mAppView = appView;
    	    }

    	    public void doEchoTest(String echo){
    	        Toast toast = Toast.makeText(mAppView.getContext(), echo, Toast.LENGTH_SHORT);
    	        toast.show();
    	    }
    	}
	
}

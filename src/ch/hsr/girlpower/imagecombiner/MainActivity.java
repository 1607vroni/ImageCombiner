package ch.hsr.girlpower.imagecombiner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import eu.janmuller.android.simplecropimage.CropImage;

public class MainActivity extends Activity {
	
	private Uri uriImage;
	private Uri uriFolder;
	private CustomLayout costumlayout;
	private AlertDialog.Builder alert;
	private File image;

     public static final String TAG = "MainActivity"; 
     
	 public static final int REQUEST_CODE_GALLERY      = 0x1;
	 public static final int REQUEST_CODE_TAKE_PICTURE = 0x2;
	 public static final int REQUEST_CODE_CROP_IMAGE   = 0x3;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		costumlayout = (CustomLayout) findViewById(R.id.mainlayout);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		MenuItem menuItem = menu.add("Kamera");
		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				takePicture();
				return false;
			}
		});
		
		MenuItem menuItem_gallery = menu.add("Gallery");
		menuItem_gallery.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				openGallery();
				return false;
			}
		});

		MenuItem menuItem_logbuch = menu.add("Log-Buch");
		menuItem_logbuch.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				sendAlert();
				return false;
			}
		});
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    switch (requestCode) {
	    case REQUEST_CODE_TAKE_PICTURE:
	        if (resultCode == Activity.RESULT_OK) {
	            Uri selectedImage = uriImage;
	            getContentResolver().notifyChange(selectedImage, null);
	            ContentResolver cr = getContentResolver();
	            Bitmap bitmap;
	            try {
	                 bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);
	            } catch (Exception e) {
	                Toast.makeText(this, "Bild nicht geladen", Toast.LENGTH_SHORT).show();
	                Log.e("Camera", e.toString());
	            }
	            startCropImage();
	        }
	        break;
        case REQUEST_CODE_GALLERY:
        	if (resultCode == Activity.RESULT_OK) {
	            Uri selectedImage = uriImage;
	            Bitmap bitmap;

	            try {
	                InputStream inputStream = getContentResolver().openInputStream(data.getData());
					FileOutputStream fileOutputStream = new FileOutputStream(image);
	                copyStream(inputStream, fileOutputStream);
	                fileOutputStream.close();
	                inputStream.close();

	            } catch (Exception e) {
	                Toast.makeText(this, "Bild nicht geladen", Toast.LENGTH_SHORT).show();
	                Log.e("Camera", e.toString());
	            }
	           
	            startCropImage();
	        }
            break;
	    case REQUEST_CODE_CROP_IMAGE:
	    	if (resultCode == Activity.RESULT_OK) {
	    		String path = uriImage.getPath();
	    		Bitmap bitmap = BitmapFactory.decodeFile(path);
	    		
	            TouchImageView tiv = new TouchImageView(this);
	            
	            tiv.setBitmap(modPic(bitmap));
	            costumlayout.addView(tiv);
	    	}
            break;
	    }
	}
	
	private void takePicture(){
		
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		File imagesFolder = new File(Environment.getExternalStorageDirectory(), "MyImages");
        imagesFolder.mkdirs(); 
        uriFolder = Uri.fromFile(imagesFolder);
        File image = new File(imagesFolder, "image_001.jpg");
        uriImage = Uri.fromFile(image);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriImage);

	    startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
	}
	
	
    private void openGallery() {

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
    }
    
    private void startCropImage() {

        Intent intent = new Intent(this, CropImage.class);
        
        String filePath = uriImage.getPath();
	    intent.putExtra(CropImage.IMAGE_PATH, filePath);
	    intent.putExtra(CropImage.SCALE, true);

        intent.putExtra(CropImage.ASPECT_X, 3);
        intent.putExtra(CropImage.ASPECT_Y, 2);

        startActivityForResult(intent, REQUEST_CODE_CROP_IMAGE);
    }
	
	private Bitmap modPic(Bitmap pic){
		Bitmap bmOut;
		
		bmOut = doBrightness(pic, 50);
		bmOut = doContrast(bmOut, 100);
		
		return bmOut;
	}
	
	private Bitmap doContrast(Bitmap pic, double value){
		// image size
	    int width = pic.getWidth();
	    int height = pic.getHeight();
	    // create output bitmap

	    // create a mutable empty bitmap
	    Bitmap bmOut = Bitmap.createBitmap(width, height, pic.getConfig());

	    // create a canvas so that we can draw the bmOut Bitmap from source bitmap
	    Canvas c = new Canvas();
	    c.setBitmap(bmOut);

	    // draw bitmap to bmOut from src bitmap so we can modify it
	    c.drawBitmap(pic, 0, 0, new Paint(Color.BLACK));


	    // color information
	    int A, R, G, B;
	    int pixel;
	    // get contrast value
	    double contrast = Math.pow((100 + value) / 100, 2);

	    // scan through all pixels
	    for(int x = 0; x < width; ++x) {
	        for(int y = 0; y < height; ++y) {
	            // get pixel color
	            pixel = pic.getPixel(x, y);
	            A = Color.alpha(pixel);
	            // apply filter contrast for every channel R, G, B
	            R = Color.red(pixel);
	            R = (int)(((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
	            if(R < 0) { R = 0; }
	            else if(R > 255) { R = 255; }

	            G = Color.green(pixel);
	            G = (int)(((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
	            if(G < 0) { G = 0; }
	            else if(G > 255) { G = 255; }

	            B = Color.blue(pixel);
	            B = (int)(((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
	            if(B < 0) { B = 0; }
	            else if(B > 255) { B = 255; }

	            // set new pixel color to output bitmap
	            bmOut.setPixel(x, y, Color.argb(A, R, G, B));
	        }
	    }
	    return bmOut;
	}
	
	private Bitmap doBrightness(Bitmap src, int value) {
	    // image size
	    int width = src.getWidth();
	    int height = src.getHeight();
	    // create output bitmap
	    Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
	    // color information
	    int A, R, G, B;
	    int pixel;
	 
	    // scan through all pixels
	    for(int x = 0; x < width; ++x) {
	        for(int y = 0; y < height; ++y) {
	            // get pixel color
	            pixel = src.getPixel(x, y);
	            A = Color.alpha(pixel);
	            R = Color.red(pixel);
	            G = Color.green(pixel);
	            B = Color.blue(pixel);
	 
	            // increase/decrease each channel
	            R += value;
	            if(R > 255) { R = 255; }
	            else if(R < 0) { R = 0; }
	 
	            G += value;
	            if(G > 255) { G = 255; }
	            else if(G < 0) { G = 0; }
	 
	            B += value;
	            if(B > 255) { B = 255; }
	            else if(B < 0) { B = 0; }
	 
	            // apply new pixel color to output bitmap
	            bmOut.setPixel(x, y, Color.argb(A, R, G, B));
	        }
	    }
	 
	    // return final image
	    return bmOut;
	}
	
	/**
	Log-Buch
	 * */
			private void sendlog(String lwort) {
				Intent intent = new Intent("ch.appquest.intent.LOG");
				 
				if (getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
					Toast.makeText(this, "Logbook App not Installed", Toast.LENGTH_LONG).show();
				return;
				}
				 
				intent.putExtra("ch.appquest.taskname", "Bildkombinierer");
				intent.putExtra("ch.appquest.logmessage", lwort);
				 
				startActivity(intent);
			}
			
	private void sendAlert(){


		//AlertDialog
		alert = new AlertDialog.Builder(this);

		alert.setTitle("Logbuch-Eintrag");
		alert.setMessage("Lösungswort eintragen");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String lwort = (String) input.getText().toString();
		  sendlog(lwort);
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});
		alert.show();
	}
	
    public static void copyStream(InputStream input, OutputStream output)
            throws IOException {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

}

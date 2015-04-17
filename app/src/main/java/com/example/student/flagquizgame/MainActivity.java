package com.example.student.signquizgame;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import java.io.IOException; //for reading image files from assets folder
import java.io.InputStream; //for reading image files from assets folder
import java.util.ArrayList; //for holding image file names and current quiz items
import java.util.Collections; //for shuffle method
import java.util.HashMap; //storing region names and corresponding Boolean values indicating whether each region is enabled or disabled.
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager; //for accessing files in assets folder
import android.graphics.drawable.Drawable; //for displaying the image file in an ImageView after reading it in.
import android.os.Handler; //used to execute a Runnable object in the future
import android.util.Log;  //used for logging exceptions for debugging purposes – viewed by using the Android logcat tool and are also displayed in the Android DDMS (Dalvik Debug Monitor Server) perspective’s LogCat tab in Eclipse.
import android.view.LayoutInflater;
import android.view.MenuItem; //along with Menu class used to display a context menu.
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;  //used for the animations
import android.view.animation.AnimationUtils;  //used for the animations
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.GestureDetector.*;


public class MainActivity extends ActionBarActivity {

    // String used when logging error messages
    private static final String TAG = "SignQuizGame Activity";
//
    private List<String> fileNameList; // sign file names
    private List<String> quizSignsList; // names of sign in quiz
    private Map<String, Boolean> signsMap; // which signs are enabled
    private String correctAnswer; // correct country for the current sign
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows; // number of rows displaying choices
    private Random random; // random number generator
    private Handler handler; // used to delay loading next sign
    private Animation shakeAnimation; // animation for incorrect guess
//
    private TextView answerTextView; // displays Correct! or Incorrect!
    private TextView questionNumberTextView; // shows current question #
    private ImageView signImageView; // displays a sign
    private TableLayout buttonTableLayout; // table of answer Buttons

    // create constants for each menu id
    private final int CHOICES_MENU_ID = Menu.FIRST;
    private final int RESET_MENU_ID = Menu.FIRST + 1;

    //GestureDetector
    private GestureDetector gestureDetector;    //listen for double taps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileNameList = new ArrayList<String>();
        quizSignsList = new ArrayList<String>();
        signsMap = new HashMap<String, Boolean>();
        guessRows = 1; //default to 1 row of choices (1 row buttons w/ 3 possible answers)
        random = new Random();
        handler = new Handler(); //used to perform delayed operations (ie: threads)

        //load the animation to use for incorrect guesses
        //this = the game activity for this java file
        shakeAnimation =
                AnimationUtils.loadAnimation(this, R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        //dynamically get array of signs
        String[] signNames =
                getResources().getStringArray(R.array.signNamesList);

        //by default, signs are choosen from all
        for (String signName : signNames) {
            signsMap.put(signName, true); //signs are enabled initially, then use menu to change it
        }

        //get references to GUI components
        questionNumberTextView = (TextView) findViewById(R.id.questionNumberTextView);
        signImageView = (ImageView) findViewById(R.id.signImageView);
        buttonTableLayout = (TableLayout) findViewById(R.id.buttonTableLayout);
        answerTextView = (TextView) findViewById(R.id.answerTextView);

        //set questionNumberTextView's text
        questionNumberTextView.setText(
                getResources().getString(R.string.question) + " 1 " +
                getResources().getString(R.string.of) + " 10 "
        );

        //initialize GestureDetector
        gestureDetector = new GestureDetector(this, gestureListener);

        //start a new quiz
        resetQuiz();
    }

    private void resetQuiz() {
        //use AssetManager to get sign image
        AssetManager assets = getAssets(); //get the app's assetManager
        fileNameList.clear(); //empty the list
        try {
            String[] paths = assets.list("signs");

            for(String path: paths) {
                fileNameList.add(path.replace(".png", ""));
            }

        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }
//
        //reset number of correct answers
        correctAnswers = 0;
        //reset number of guesses
        totalGuesses = 0;
        //clear current quiz sign list
        quizSignsList.clear();

        //add 10 random file names to quizSignsList
        int signCounter = 1;
        int numberOfSigns = fileNameList.size();

        while (signCounter <= 10) {
            int randomIndex = random.nextInt(numberOfSigns); //random index
            //get the random file name
            String fileName = fileNameList.get(randomIndex);

            //if the sign hasn't already been chosen
            if(!quizSignsList.contains(fileName)) {
                //add it to quizSignsList
                quizSignsList.add(fileName);
                ++signCounter;
            }
        } //while

        //start the quiz by loading the first sign
        loadNextSign();
    } //resetQuiz

    // after the user guesses a correct sign, load the next sign
    private void loadNextSign()
    {
        // get file name of the next sign and remove it from the list
        String nextImageName = quizSignsList.remove(0);
        correctAnswer = nextImageName; // update the correct answer

        answerTextView.setText(""); // clear answerTextView

        // display the number of the current question in the quiz
        questionNumberTextView.setText(
                getResources().getString(R.string.question) + " " +
                        (correctAnswers + 1) + " " +
                        getResources().getString(R.string.of) + " 10");

        // use AssetManager to load next image from assets folder
        AssetManager assets = getAssets(); // get app's AssetManager
        InputStream stream; // used to read in sign images

        try
        {
            // get an InputStream to the asset representing the next flag
            nextImageName = nextImageName.replace(" ", "_");
            stream = assets.open("signs/" + nextImageName + ".png");

            // load the asset as a Drawable and display on the signImageView
            Drawable sign = Drawable.createFromStream(stream, nextImageName);
            signImageView.setImageDrawable(sign);
        } // end try
        catch (IOException e)
        {
            Log.e(TAG, "Error loading " + nextImageName, e);
        } // end catch

        // clear prior answer Buttons from TableRows
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
            ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();

        Collections.shuffle(fileNameList); // shuffle file names


        // put the correct answer at the end of fileNameList later will be inserted randomly into the answer Buttons
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // get a reference to the LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // add 3, 6, or 9 answer Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++)
        {
            TableRow currentTableRow = getTableRow(row); //obtains the TableRow at a specific index in the buttonTableLayout

            // place Buttons in currentTableRow
            for (int column = 0; column < 3; column++)
            {
                // inflate guess_button.xml to create new Button
                Button newGuessButton =
                        (Button) inflater.inflate(R.layout.guess_button, null);

                // get sign name and set it as newGuessButton's text
                String fileName = fileNameList.get((row * 3) + column);
                newGuessButton.setText(getSignName(fileName));

                // register answerButtonListener to respond to button clicks
                newGuessButton.setOnClickListener(guessButtonListener);
                currentTableRow.addView(newGuessButton);
            } // end for
        } // end for

        // randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(3); // pick random column
        TableRow randomTableRow = getTableRow(row); // get the TableRow
        String signName = getSignName(correctAnswer);
        ((Button)randomTableRow.getChildAt(column)).setText(signName);
    } // end method loadNextFlag

    // returns the specified TableRow
    private TableRow getTableRow(int row)
    {
        return (TableRow) buttonTableLayout.getChildAt(row);
    } // end method getTableRow

    // parses the sign file name and returns the sign name
    private String getSignName(String name)
    {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    } // end method getSignName

    // called when the user selects an answer
    private void submitGuess(Button guessButton)
    {
        String guess = guessButton.getText().toString();
        String answer = getSignName(correctAnswer);
        ++totalGuesses; // increment the number of guesses the user has made

        // if the guess is correct
        if (guess.equals(answer))
        {
            ++correctAnswers; // increment the number of correct answers

            // display "Correct!" in green text
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
                    getResources().getColor(R.color.correct_answer));

            disableButtons(); // disable all answer Buttons

            // if the user has correctly identified 10 signs
            if (correctAnswers == 10)
            {
                // create a new AlertDialog Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.reset_quiz); // title bar string

                // set the AlertDialog's message to display game results
                builder.setMessage(String.format("%d %s, %.02f%% %s",
                        totalGuesses, getResources().getString(R.string.guesses),
                        (1000 / (double) totalGuesses),
                        getResources().getString(R.string.correct)));

                builder.setCancelable(false);

                // add "Reset Quiz" Button
                builder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                resetQuiz();
                            } // end method onClick
                        } // end anonymous inner class
                ); // end call to setPositiveButton

                // create AlertDialog from the Builder
                AlertDialog resetDialog = builder.create();
                resetDialog.show(); // display the Dialog
            } // end if
            else // answer is correct but quiz is not over
            {
                // load the next flag after a 1-second delay
                handler.postDelayed(
                        //anonymous inner class implementing Runnable
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                loadNextSign();
                            }
                        }, 1000); // 1000 milliseconds for 1-second delay
            } // end else
        } // end if
        else // guess was incorrect
        {
            // play the animation
            signImageView.startAnimation(shakeAnimation);

            // display "Incorrect!" in red
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(
                    getResources().getColor(R.color.incorrect_answer));
            guessButton.setEnabled(false); // disable the incorrect answer
        } // end else
    } // end method submitGuess

    // utility method that disables all answer Buttons
    private void disableButtons()
    {
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
        {
            TableRow tableRow = (TableRow) buttonTableLayout.getChildAt(row);
            for (int i = 0; i < tableRow.getChildCount(); ++i)
                tableRow.getChildAt(i).setEnabled(false);
        } // end outer for
    } // end method disableButtons


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);

        //create our own fucking menu
        super.onCreateOptionsMenu(menu);

        //add two options to the menu
        //add(groupID, uniqueItemID, orderItemsShouldAppear, stringOfText)
        menu.add(Menu.NONE, CHOICES_MENU_ID, Menu.NONE, R.string.choices);
        menu.add(Menu.NONE, RESET_MENU_ID, Menu.NONE, R.string.reset_quiz);

        //display the menu
        return true;

    }

    //called when user selects an option from the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //get unique id of item
        switch(item.getItemId()) {
            case CHOICES_MENU_ID :
                //create a list of the possible number of answer choices
                final String[] possibleChoices =
                        getResources().getStringArray(R.array.guessesList);
                AlertDialog.Builder choicesBuilder =
                        new AlertDialog.Builder(this);
                choicesBuilder.setTitle(R.string.choices);

                //add possible choices to the Dialog
                choicesBuilder.setItems(R.array.guessesList,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                //update guessRows to math the user's choice
                                guessRows = Integer.parseInt(
                                        possibleChoices[item].toString())/3;
                                resetQuiz();
                            } //onClick
                        }); //setItems

                AlertDialog choicesDialog = choicesBuilder.create();
                choicesDialog.show();
                return true;

            case RESET_MENU_ID :
                //create another dialog
                AlertDialog.Builder resetBuilder =
                        new AlertDialog.Builder(this);
                resetBuilder.setTitle(R.string.reset_quiz);

                //reset quiz on positive button
                resetBuilder.setPositiveButton(R.string.reset,
                        new DialogInterface.OnClickListener() {
                            @Override
                        public void onClick(DialogInterface dialog, int which) {
                                resetQuiz();
                            }
                        }); //set Positive button
                AlertDialog resetDialog = resetBuilder.create();
                resetDialog.show();
                return true;
        } //end switch

        return super.onOptionsItemSelected(item);
    }

    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            submitGuess((Button) v);
        }
    };

    //when the user touches the screen
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //get the int representing the action type
        int action = event.getAction();

        //call the GestureDetector's onTouchEvent to check for double taps
        return gestureDetector.onTouchEvent(event);
    }

    //listen for touch events sent to the GestureDetector
    SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //fire ze intent
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.pdclipart.org/thumbnails.php?album=104"));
                startActivity(browserIntent);
                return true;
            } catch (ActivityNotFoundException excep) {
                excep.printStackTrace();
                return false;
            }
        }
    };

}

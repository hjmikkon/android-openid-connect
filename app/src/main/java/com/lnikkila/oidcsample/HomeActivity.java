package com.lnikkila.oidcsample;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.lnikkila.oidcsample.oidc.authenticator.Authenticator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Initiates the login procedures and contains all UI stuff related to the main activity.
 *
 * @author Leo Nikkilä
 */
public class HomeActivity extends Activity {

    private Button loginButton;
    private ProgressBar progressBar;

    private AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        loginButton = (Button) findViewById(R.id.loginButton);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        accountManager = AccountManager.get(this);
    }

    /**
     * Called when the user taps the big yellow button.
     */
    public void doLogin(final View view) {
        // Grab all our accounts
        final String accountType = getString(R.string.ACCOUNT_TYPE);
        final Account availableAccounts[] = accountManager.getAccountsByType(accountType);

        switch (availableAccounts.length) {
            // No account has been created, let's create one now
            case 0:
                createDiscoveryDialog(view, this, accountType);
                break;

            default:
                String name[] = new String[availableAccounts.length + 1];

                for (int i = 0; i < availableAccounts.length; i++) {
                    name[i] = availableAccounts[i].name;
                }
                name[availableAccounts.length] = "Add new one";
                final HomeActivity homeActivity = this;

                new AlertDialog.Builder(this)
                        .setTitle("Choose an account")
                        .setAdapter(new ArrayAdapter<>(this,
                                        android.R.layout.simple_list_item_1, name),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int selectedAccount) {
                                        if (selectedAccount < availableAccounts.length) {
                                            new ApiTask().execute(availableAccounts[selectedAccount]);
                                        } else {
                                            createDiscoveryDialog(view, homeActivity, accountType);
                                        }
                                    }
                                })
                        .create()
                        .show();
        }
    }

    private class ApiTask extends AsyncTask<Account, Void, Map> {

        @Override
        protected void onPreExecute() {
            loginButton.setText("");
            progressBar.setVisibility(View.VISIBLE);
        }

        /**
         * Makes the API request. We could use the OIDCUtils.getUserInfo() method, but we'll do it
         * like this to illustrate making generic API requests after we've logged in.
         */
        @Override
        protected Map doInBackground(Account... args) {
            Account account = args[0];

            try {
                return APIUtility.getJson(HomeActivity.this, Config.userInfoUrl, account);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Processes the API's response.
         */
        @Override
        protected void onPostExecute(Map result) {
            progressBar.setVisibility(View.INVISIBLE);

            if (result == null) {
                loginButton.setText("Couldn't get user info");
            } else {
                loginButton.setText("Logged in as " + result.get("sub"));
            }
        }

    }

    protected void createDiscoveryDialog(final View view, final Activity homeActivity, final String accountType) {
        String[] methods = new String[5];
        methods[0] = "Local Password";
        methods[1] = "CGI";
        methods[2] = "GlobalSign";
        methods[3] = "Google";
        methods[4] = "Dynamic";
        new AlertDialog.Builder(homeActivity)
                .setTitle("Choose method")
                .setAdapter(new ArrayAdapter<>(homeActivity,
                                android.R.layout.simple_list_item_1, methods),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog2, int selectedMethod) {
                                createLoginHintDialog(view, homeActivity, accountType, selectedMethod);
                                /*)
                                String[] discovery = null;
                                if (selectedMethod < 4) {
                                    discovery = new String[1];
                                    switch (selectedMethod) {
                                        case 0:
                                            discovery[0] = "authn/Password";
                                            break;
                                        case 1:
                                            discovery[0] = "authn/SocialUserCgi";
                                            break;
                                        case 2:
                                            discovery[0] = "authn/SocialUserGss";
                                            break;
                                        case 3:
                                            discovery[0] = "authn/SocialUserGoogle";
                                            break;
                                    }
                                    discovery[0] = encodeDiscovery("{ \"authnMethod\"=\"" + discovery[0] + "\", \"username\"=\"test\" }");
                                }
                                AccountManagerFuture<Bundle> future = accountManager.addAccount(accountType, Authenticator.TOKEN_TYPE_ID, discovery, null,
                                        homeActivity, new AccountManagerCallback<Bundle>() {
                                            @Override
                                            public void run(AccountManagerFuture<Bundle> futureManager) {
                                                // Unless the account creation was cancelled, try logging in again
                                                // after the account has been created.
                                                if (futureManager.isCancelled())
                                                    return;
                                                doLogin(view);
                                            }
                                        }, null);*/

                            }
                        }

                )

                .create()
                .show();

    }


    protected String encodeDiscovery(final String input) {
        byte[] data = null;
        try {
            data = input.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e1) {
            return input;
        }
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    protected void createLoginHintDialog(final View view, final Activity homeActivity, final String accountType, final int selectedMethod) {

        final String[] discovery;
        if (selectedMethod < 4) {
            discovery = new String[1];
            switch (selectedMethod) {
                case 0:
                    discovery[0] = "authn/Password";
                    break;
                case 1:
                    discovery[0] = "authn/SocialUserCgi";
                    break;
                case 2:
                    discovery[0] = "authn/SocialUserGss";
                    break;
                case 3:
                    discovery[0] = "authn/SocialUserGoogle";
                    break;
            }
//            discovery[0] = encodeDiscovery("{ \"authnMethod\"=\"" + discovery[0] + "\", \"username\"=\"test\" }");
            discovery[0] = "{ \"authnMethod\"=\"" + discovery[0] + "\"";
        } else {
            discovery = null;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(homeActivity)
                .setTitle("Give login hint");
        final EditText input = new EditText(homeActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String[] fullDiscovery = discovery;
                if (fullDiscovery != null) {
                    fullDiscovery[0] = encodeDiscovery(fullDiscovery[0] + "\"username\"=\"" + input.getText().toString() + "\" }");
                }

                AccountManagerFuture<Bundle> future = accountManager.addAccount(accountType, Authenticator.TOKEN_TYPE_ID, fullDiscovery, null,
                        homeActivity, new AccountManagerCallback<Bundle>() {
                            @Override
                            public void run(AccountManagerFuture<Bundle> futureManager) {
                                // Unless the account creation was cancelled, try logging in again
                                // after the account has been created.
                                if (futureManager.isCancelled())
                                    return;
                                doLogin(view);
                            }
                        }, null);

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();

    }
}
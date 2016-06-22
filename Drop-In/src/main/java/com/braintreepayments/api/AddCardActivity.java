package com.braintreepayments.api;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.dropin.interfaces.AddPaymentUpdateListener;
import com.braintreepayments.api.dropin.view.AddCardView;
import com.braintreepayments.api.dropin.view.EditCardView;
import com.braintreepayments.api.dropin.view.EnrollmentCardView;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.PaymentMethodNonce;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AddCardActivity extends AppCompatActivity implements AddPaymentUpdateListener,
        PaymentMethodNonceCreatedListener, BraintreeErrorListener {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            CARD_ENTRY,
            DETAILS_ENTRY,
            SUBMIT
    })
    private @interface State {}
    public static final int CARD_ENTRY = 1;
    public static final int DETAILS_ENTRY = 2;
    public static final int SUBMIT = 3;

    private Toolbar mToolbar;
    private AddCardView mAddCardView;
    private EditCardView mEditCardView;
    private EnrollmentCardView mEnrollmentCardView;

    private BraintreeFragment mBraintreeFragment;
    private final CardBuilder mCardBuilder = new CardBuilder();

    @State
    private int mState = CARD_ENTRY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bt_add_card_activity);
        mToolbar = (Toolbar)findViewById(R.id.toobar);
        mAddCardView = (AddCardView)findViewById(R.id.add_card_view);
        mEditCardView = (EditCardView)findViewById(R.id.edit_card_view);
        mEnrollmentCardView = (EnrollmentCardView)findViewById(R.id.enrollment_card_view);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mAddCardView.setAddPaymentUpdatedListener(this);
        mEditCardView.setAddPaymentUpdatedListener(this);
        mEnrollmentCardView.setAddPaymentUpdatedListener(this);

        PaymentRequest paymentRequest = getIntent().getParcelableExtra(PaymentRequest.EXTRA_CHECKOUT_REQUEST);

        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, paymentRequest.getAuthorization());
        } catch (InvalidArgumentException e) {
            // TODO alert the merchant their authorization may be incorrect.
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPaymentUpdated(final View v) {
        // Mimic network
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                int lastState = mState;
                int nextState = determineNextState(v);
                if (nextState == lastState) {
                    return;
                } else {
                    mState = nextState;
                }

                switch (lastState) {
                    case CARD_ENTRY:
                        mAddCardView.setVisibility(View.GONE);
                        mCardBuilder.cardNumber(mAddCardView.getNumber());
                        mEditCardView.setCardNumber(mAddCardView.getNumber());
                        break;
                    case DETAILS_ENTRY:
                        mEditCardView.setVisibility(View.GONE);
                        mCardBuilder.expirationDate(mEditCardView.getExpirationDate());
                        mCardBuilder.cvv(mEditCardView.getCvv());
                        break;
//                    case ENROLLMENT_ENTRY:
//                        mEnrollmentCardView.setVisibility(View.GONE);
//                        break;
                }

                switch(nextState) {
                    case CARD_ENTRY:
                        getSupportActionBar().setTitle("Enter Card Details");
                        mAddCardView.setVisibility(View.VISIBLE);
                        break;
                    case DETAILS_ENTRY:
                        getSupportActionBar().setTitle("Card Details");
                        mEditCardView.setVisibility(View.VISIBLE);
                        break;
//                    case ENROLLMENT_ENTRY:
//                        getSupportActionBar().setTitle("Confirm Enrollment");
//                        mEnrollmentCardView.setVisibility(View.VISIBLE);
//                        break;
                    case SUBMIT:
                        createCard();
                        break;
                }
            }
        }, 5000);
    }

    @State
    private int determineNextState(View v) {
        int nextState = mState;
        if (v.getId() == mAddCardView.getId() && !TextUtils.isEmpty(mAddCardView.getNumber())) {
            nextState = DETAILS_ENTRY;
        }
        else if (v.getId() == mEditCardView.getId()) {
            nextState = SUBMIT;
        }

        return nextState;
    }

    private void createCard() {
        Card.tokenize(mBraintreeFragment, mCardBuilder);
    }

    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethod) {
        Intent result = new Intent();
        result.putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethod);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    public void onError(Exception e) {
        throw new RuntimeException(e);
    }
}
/*
 * Copyright 2013 Jake Wharton
 * Copyright 2014 Prateek Srivastava (@f2prateek)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.f2prateek.dart.example;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.f2prateek.dart.Dart;
import com.f2prateek.dart.HensonNavigable;
import com.f2prateek.dart.InjectExtra;
import com.f2prateek.dart.Nullable;

@HensonNavigable(model = SampleModelActivity.Model.class) public class SampleModelActivity
    extends Activity {

  public static final String TAG_SAMPLE_FRAGMENT = "SampleFragment";
  @InjectView(R.id.default_key_extra) TextView defaultKeyExtraTextView;
  @InjectView(R.id.string_extra) TextView stringExtraTextView;
  @InjectView(R.id.int_extra) TextView intExtraTextView;
  @InjectView(R.id.parcelable_extra) TextView parcelableExtraTextView;
  @InjectView(R.id.optional_extra) TextView optionalExtraTextView;
  @InjectView(R.id.parcel_extra) TextView parcelExtraTextView;
  @InjectView(R.id.default_extra) TextView defaultExtraTextView;

  private Model model = new Model();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sample);

    ButterKnife.inject(this);
    Dart.inject(model, this);

    // Contrived code to use the "injected" extras.
    stringExtraTextView.setText(model.stringExtra);
    intExtraTextView.setText(String.valueOf(model.intExtra));
    parcelableExtraTextView.setText(String.valueOf(model.parcelableExtra));
    optionalExtraTextView.setText(String.valueOf(model.optionalExtra));
    parcelExtraTextView.setText(String.valueOf(model.parcelExtra.getName()));
    defaultExtraTextView.setText(String.valueOf(model.defaultExtra));
    defaultKeyExtraTextView.setText(model.defaultKeyExtra);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Fragment sampleFragment = getFragmentManager().findFragmentByTag(TAG_SAMPLE_FRAGMENT);
    if (sampleFragment == null) {
      getFragmentManager().beginTransaction()
          .add(new SampleFragment(), TAG_SAMPLE_FRAGMENT)
          .commit();
    }
  }

  public static class Model {
    public static final String DEFAULT_EXTRA_VALUE = "a default value";

    private static final String EXTRA_STRING = "extraString";
    private static final String EXTRA_INT = "extraInt";
    private static final String EXTRA_PARCELABLE = "extraParcelable";
    private static final String EXTRA_OPTIONAL = "extraOptional";
    private static final String EXTRA_PARCEL = "extraParcel";
    private static final String EXTRA_WITH_DEFAULT = "extraWithDefault";

    @InjectExtra(EXTRA_STRING) String stringExtra;
    @InjectExtra(EXTRA_INT) int intExtra;
    @InjectExtra(EXTRA_PARCELABLE) ComplexParcelable parcelableExtra;
    @InjectExtra(EXTRA_PARCEL) ExampleParcel parcelExtra;
    @Nullable @InjectExtra(EXTRA_OPTIONAL) String optionalExtra;
    @Nullable @InjectExtra(EXTRA_WITH_DEFAULT) String defaultExtra = DEFAULT_EXTRA_VALUE;
    @InjectExtra String defaultKeyExtra;
  }
}

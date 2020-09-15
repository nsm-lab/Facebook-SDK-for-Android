/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.suggestedevents;

import static com.facebook.appevents.internal.ViewHierarchyConstants.*;

import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TimePicker;
import com.facebook.appevents.codeless.internal.ViewHierarchy;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@AutoHandleExceptions
class SuggestedEventViewHierarchy {
  static final String TAG = SuggestedEventViewHierarchy.class.getCanonicalName();
  private static final List<Class<? extends View>> blacklistedViews =
      new ArrayList<>(
          Arrays.asList(
              Switch.class,
              Spinner.class,
              DatePicker.class,
              TimePicker.class,
              RadioGroup.class,
              RatingBar.class,
              EditText.class,
              AdapterView.class));

  static JSONObject getDictionaryOfView(View view, View clickedView) {
    JSONObject json = new JSONObject();
    try {
      if (view == clickedView) {
        json.put(IS_INTERACTED_KEY, true);
      }
      updateBasicInfo(view, json);

      JSONArray childViews = new JSONArray();
      List<View> children = ViewHierarchy.getChildrenOfView(view);
      for (int i = 0; i < children.size(); i++) {
        View child = children.get(i);
        JSONObject childInfo = getDictionaryOfView(child, clickedView);
        childViews.put(childInfo);
      }
      json.put(CHILDREN_VIEW_KEY, childViews);
    } catch (JSONException e) {
      /*no op*/
    }

    return json;
  }

  static void updateBasicInfo(View view, JSONObject json) {
    try {
      String text = ViewHierarchy.getTextOfView(view);
      String hint = ViewHierarchy.getHintOfView(view);

      json.put(CLASS_NAME_KEY, view.getClass().getSimpleName());
      json.put(CLASS_TYPE_BITMASK_KEY, ViewHierarchy.getClassTypeBitmask(view));
      if (!text.isEmpty()) {
        json.put(TEXT_KEY, text);
      }
      if (!hint.isEmpty()) {
        json.put(HINT_KEY, hint);
      }
      if (view instanceof EditText) {
        json.put(INPUT_TYPE_KEY, ((EditText) view).getInputType());
      }
    } catch (JSONException e) {
      /*no op*/
    }
  }

  static List<View> getAllClickableViews(View view) {
    List<View> clickableViews = new ArrayList<>();

    for (Class<? extends View> viewClass : blacklistedViews) {
      if (viewClass.isInstance(view)) {
        return clickableViews;
      }
    }

    if (view.isClickable()) {
      clickableViews.add(view);
    }

    List<View> children = ViewHierarchy.getChildrenOfView(view);
    for (View child : children) {
      clickableViews.addAll(getAllClickableViews(child));
    }
    return clickableViews;
  }

  static String getTextOfViewRecursively(View hostView) {
    String text = ViewHierarchy.getTextOfView(hostView);
    if (!text.isEmpty()) {
      return text;
    }
    List<String> childrenText = getTextOfChildren(hostView);
    return TextUtils.join(" ", childrenText);
  }

  private static List<String> getTextOfChildren(View view) {
    List<String> childrenText = new ArrayList<>();
    List<View> childrenView = ViewHierarchy.getChildrenOfView(view);
    for (View childView : childrenView) {
      String childText = ViewHierarchy.getTextOfView(childView);
      if (!childText.isEmpty()) {
        childrenText.add(childText);
      }
      childrenText.addAll(getTextOfChildren(childView));
    }
    return childrenText;
  }
}

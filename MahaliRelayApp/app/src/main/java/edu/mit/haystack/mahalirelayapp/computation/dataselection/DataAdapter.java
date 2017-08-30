package edu.mit.haystack.mahalirelayapp.computation.dataselection;

/* The MIT License (MIT)
 * Copyright (c) 2015 Massachusetts Institute of Technology
 *
 * Author: David Mascharka
 * This software is part of the Mahali Project, PI: V. Pankratius
 * http://mahali.mit.edu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

import edu.mit.haystack.mahalirelayapp.R;

/**
 * @author David Mascharka
 *
 * ArrayAdapter for holding data entries (data_entry.xml, DataEntry.java)
 */
public class DataAdapter extends ArrayAdapter<DataEntry> {
    private ArrayList<DataEntry> data;
    private Activity context;

    public DataAdapter(Activity context, ArrayList<DataEntry> dataList) {
        super(context, R.layout.data_entry, dataList);
        this.context = context;
        data = dataList;
    }

    private static class ViewHolder {
        protected TextView text;
        protected CheckBox checkBox;
        protected TextView filePath;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            convertView = inflater.inflate(R.layout.data_entry, null);

            viewHolder = new ViewHolder();
            viewHolder.text = (TextView) convertView.findViewById(R.id.data_file_name);
            viewHolder.filePath = (TextView) convertView.findViewById(R.id.data_file_path);
            viewHolder.checkBox = (CheckBox) convertView.findViewById(R.id.data_check);
            viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = (int) buttonView.getTag();
                    data.get(position).setChecked(buttonView.isChecked());
                }
            });

            convertView.setTag(viewHolder);
            convertView.setTag(R.id.data_file_name, viewHolder.text);
            convertView.setTag(R.id.data_file_path, viewHolder.filePath);
            convertView.setTag(R.id.data_check, viewHolder.checkBox);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.checkBox.setTag(position);
        viewHolder.filePath.setText(data.get(position).getFilePath());
        viewHolder.text.setText(data.get(position).getFileName());
        viewHolder.checkBox.setChecked(data.get(position).isChecked());

        return convertView;
    }
}

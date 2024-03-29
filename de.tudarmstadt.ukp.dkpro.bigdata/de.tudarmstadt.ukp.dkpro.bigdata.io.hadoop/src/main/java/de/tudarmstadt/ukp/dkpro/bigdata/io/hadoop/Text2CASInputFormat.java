/*******************************************************************************
 * Copyright 2013
 * TU Darmstadt, FG Sprachtechnologie
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.bigdata.io.hadoop;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

/**
 * Input format for generating CAS instances from <Text, Text> key/value pairs
 * 
 * By default, the value of the <Text, Text> key/value lines in the input files is used as CAS
 * document text. See {@link setDocumentTextExtractorClass} to change this behavior.
 * 
 * @author Johannes Simon
 * 
 */
public class Text2CASInputFormat
extends FileInputFormat<Text, CASWritable>
{

	/**
	 * Provide a custom implementation of this interface if you want the generated CAS instances to
	 * contain text different from the value of <Text, Text> key/value lines in the input files.<br>
	 * <br>
	 * This is useful e.g. if you want to remove HTML markup or if you want to extract text from the
	 * <b>key</b>, not from the <b>value</b>.<br>
	 * <br>
	 * By default, i.e. if you do not set a custom DocumentTextExtractor, the input line's value is
	 * used as the CAS document text.
	 */
	public interface DocumentTextExtractor
	{
		Text extractDocumentText(Text key, Text value);
	}

	/**
	 * Tells Text2CASInputFormat to use a custom implementation of DocumentTextExtractor. <br>
	 * By default, i.e. if you do not set a custom DocumentTextExtractor, the input line's value is
	 * used as the CAS document text.
	 * 
	 * @param conf
	 *            Configuration object
	 * @param extractorClass
	 *            Implementation of DocumentTextExtractor
	 */
	public static void setDocumentTextExtractorClass(Configuration conf,
			Class<? extends DocumentTextExtractor> extractorClass)
	{
		conf.set("dkpro.uima.text2casinputformat.documenttextextractor", extractorClass.getName());
	}

	/**
	 * Reads in CAS from XML or plain text.<br/>
	 * <br/>
	 * This is an internal class. It is only visible to hadoop.io package.
	 * 
	 * @author Johannes Simon
	 * 
	 */
	private class Text2CASRecordReader
	extends GenericKeyValueLineRecordReader<Text, CASWritable>
	{
		private final DocumentTextExtractor textExtractor;

		public Text2CASRecordReader(FileSplit fileSplit, JobConf jobConf, Reporter reporter,
				DocumentTextExtractor textExtractor)
						throws IOException
						{
			super(fileSplit, jobConf, reporter);
			this.textExtractor = textExtractor;
						}

		@Override
		public Text createKey()
		{
			return new Text();
		}

		@Override
		public CASWritable createValue()
		{
			return new CASWritable();
		}

		@Override
		public void convertKey(Text keyFrom, Text valueFrom, Text keyTo)
		{
			keyTo.set(keyFrom);
		}

		@Override
		public void convertValue(Text keyFrom, Text valueFrom, CASWritable valueTo)
		{
			CAS cas = valueTo.getCAS();
			cas.reset();
			Text doc = valueFrom;
			if (textExtractor != null)
				doc = textExtractor.extractDocumentText(keyFrom, valueFrom);
			cas.setDocumentText(doc.toString());
			try {
				// add some simple metadata
				String key_as_str = keyFrom.toString();
				String key_abbrev = StringUtils.abbreviate(key_as_str, 50);
				DocumentMetaData metadata = DocumentMetaData.create(cas);
				metadata.setDocumentTitle(key_abbrev);
				metadata.setDocumentId(String.format("<%d>%s", key_as_str.hashCode(), key_abbrev));
			} catch (Exception e) {
				System.err.println("DocumentMetaData already present.");
			}

		}
	}

	@Override
	public RecordReader<Text, CASWritable> getRecordReader(InputSplit split, JobConf jobConf,
			Reporter reporter)
					throws IOException
					{
		DocumentTextExtractor textConverter = null;
		String textConverterClass = jobConf
				.get("dkpro.uima.text2casinputformat.documenttextextractor");
		if (textConverterClass != null) {
			try {
				textConverter = (DocumentTextExtractor) Class.forName(textConverterClass)
						.newInstance();
			}
			catch (InstantiationException e) {
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return new Text2CASRecordReader((FileSplit) split, jobConf, reporter, textConverter);
					}

}

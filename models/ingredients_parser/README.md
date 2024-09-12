# Problem

PlanEat parse recipes from various internet websites

# Approaches

I tried various approaches to generate a valid model with some success/failures:

## BERTQA

Because the answer of the question is in the text, I tried to train a BERT Question&Answering model with tflite-model-maker & BertQA.

Problem, libraries are old, model was not correctly generated:

https://github.com/tensorflow/tensorflow/issues/71666

## Llama2

The second approach is to try more recent LLM models.
So based on executorch, I tried to train a llama2 model based on llama2-chat (7B).
Problem, once on Android, it was crashing:

https://github.com/pytorch/executorch/issues/4157

## Gemma

So far, the best embedded model result, because it was working on desktop. However not on android:

+ https://github.com/google-ai-edge/mediapipe/issues/5590 (Gemma 2b converted to tflite seems to produce random results)
+ https://github.com/NSTiwari/Gemma-on-Android/issues/8

Moreover, this will probably produce a big model to embed in the application. This may not be wanted.

## NYT-Parser

Finally, I found this:
Extracting Structured Data From Recipes Using Conditional Random Fields
https://archive.nytimes.com/open.blogs.nytimes.com/2015/04/09/extracting-structured-data-from-recipes-using-conditional-random-fields/?_r=0

With working code:
https://github.com/AmarOk1412/ingredient-phrase-tagger

So I took a new approach to do the parsing on cha-cu.it (anyway searching for recipes will need internet). And this avoid to get a big APK file at the end.

Model can be trained by following instructions in this repository and update
https://github.com/AmarOk1412/ingredient-phrase-tagger/blob/master/nyt-ingredients-snapshot-2015.csv

# Deploy it

Just on a server:

```
docker run -t -p 8080:8080 amarok1412/ingredient_phrase_tagger
```

And replace https://cha-cu.it/parse in the application source code to the new server.

Code is there:

https://github.com/AmarOk1412/ingredient-phrase-tagger/blob/master/bin/server.py

The main issue with this is there can't be several ingredients in a sentence (e.g. "Salt and pepper")
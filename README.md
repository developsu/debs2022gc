# debs2022gc


## description

## installation
We provide two kinds of execution environment for DEBS 22 GC testing

First one, with google colab
1. go to **google colab**
2. move to MENU [File] -> [Open Notebook] -> Upload files 'DEBS_2022_GC_GROUP_4.ipynb'

Second one, 
1. downloads the compressed zip file from github and extract it in the folder you desire.
2. Open the terminal, move to the folder, and type the following command  ( if docker is already installed, )
```
  # buid the docker image and deploy our pyspark environment/its dependencies rapidly to your system
  docker build -t pyspark:latest ./
```
3. set up your token key
4. run the debs_2022_gc.py with docker container as the following command:
```
 docker run -it pyspark:latest debs_2022_gc
```


## requirements
[Docker](https://www.docker.com) 

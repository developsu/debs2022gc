# -*- coding: utf-8 -*-
"""DEBS 2022 GC 0402_SC

Automatically generated by Colaboratory.

Original file is located at
    https://colab.research.google.com/drive/1G39sqFVZUXyx2B7-5B7kCqmHHb_ohFbd

# THINGS TO REMEMBER 
- folderDir 변경
- challenger_pb2, challenger_pb2_grpc 등록
- stream 폴더를 빈상태로 놓을 것
- Criteria: both q1 and q2 have results, last active run, benchmark type="Evaluation, benchmark run after 2022-03-17 19:40:04 UTC (switch to batchsize of 10k)"
- 리더보드에 우리 솔루션 측정치를 등록하려면 type을 **evaluation**로 바꿔서 실행

# TODO
- [X] spark ui 구축
- [X] query 2 구현, send_to_message_query2 함수 구현
- [X] large dataset이 필요한지 확인.(스트림 csv 파일이 large csv에 존재하는지 확인해야함) - 스트림 데이터셋을 이용하여 처리하면됨
- [X] 데이터셋의 크기를 늘리는 방법(현재 1000개 데이터를 받아옴, 처리량 증가 - Evaluation으로 하면 배치사이트가 증가하는 것으로 확인
- [ ] Dask csv writer를 이용하여 성능 개선, 및 파일 append? writer? 어떤 것으로 할 것인지 확인
- [ ] timestamp로 변환하는 작업의 필요성여부?
- [X] Spark 옵션 세팅 (executor, driver 등)

# Structured Streaming Program based on PySpark

## Installation
"""



import os
import findspark
findspark.init()
import pyspark
import pandas

import logging
from pyspark.sql.functions import *
from pyspark.sql.types import *
from pyspark.sql import *
from tqdm import tqdm
from dependencies.logging import Log4j
import ast

import sys
import os
from datetime import datetime
from datetime import timedelta

import csv
import pandas as pd
# from dependencies.spark import start_spark
from pyspark.sql.functions import pandas_udf, PandasUDFType
# If grpc is missing: pip install grpcio
import grpc
# from google.protobuf import empty_pb2
# If the classes below are missing, generate them:
# You need to install the grpcio-tools to generate the stubs: pip install grpcio-tools
# python -m grpc_tools.protoc -I . --python_out=. --grpc_python_out=. challenger.proto
import challenger_pb2 as ch
import challenger_pb2_grpc as api
import warnings
from google.protobuf.timestamp_pb2 import Timestamp

from pyngrok import ngrok # spark ui

"""## Core functions
This function reads the stream of csv files from remote benchmark system
"""

def read_stream(folderDir):
    """
    reads Streaming data and sets up the schema 
    :param spark session
    :return: data stream
    """
    global spark, logger
    logger.warn(f"read csv file in {folderDir}")
    stream_df = spark.readStream.format("csv") \
        .option("header", "true") \
        .schema(get_schema()) \
        .load(path=folderDir)


    return stream_df

"""This function represents the schema of stock market data"""

def get_schema():
    """
    return schema for DEBS 2022
    :return: StructType
    """
    return StructType([
        StructField("id", LongType(), True),
        StructField("symbol", StringType(), True),
        StructField("Sectype", StringType(), True),
        StructField("lasttradeprice", DoubleType(), True),
        StructField("lastTrade", TimestampType(), True),
        StructField("seconds", LongType(), True),
        StructField("nanos", LongType(), True)
    ])

"""The following functions calculate EMA38 and EMA100 indicators, respectively."""
def calcEMA_func( lastTradePrice, prev_ema, days=38):
    current_EMA = ( lastTradePrice * (2 / (days + 1)) ) + ( prev_ema  * (1 - 2 / (days + 1)) )
    return current_EMA


"""This function determines the choice of buy, sell, or stay(=hold)."""
def EMA_Comp(nEMA38_MINUS_EMA100, pEMA38_MINUS_EMA100):
    minus_prev = pEMA38_MINUS_EMA100
    minus_now = nEMA38_MINUS_EMA100
    if minus_now * minus_prev >= 0:
      return "Stay"
    else:
      if minus_now > minus_prev:
          return "Buy"

    return "Sell"

def transform_calcEMA(stream):
    # global spark, prevEMA

    stream_df = stream.select("symbol", "Sectype", "lastTrade", "lasttradeprice", "seconds", "nanos").withWatermark(
        "lastTrade", "5 minutes") \
        .groupBy(window("lastTrade", "5 minutes", "5 minutes"), col("symbol")) \
        .agg(lit(last("lasttradeprice")).alias("lasttradeprice"), lit(last("Sectype")).alias("Sectype"),
             lit(last("seconds")).alias("seconds"), lit(last("nanos")).alias("nanos")) \
        .orderBy("window")

    stream_df = stream_df.withColumn("lastWindowTime", to_timestamp(stream_df.window.end, "yyyy-MM-dd HH:mm:ss"))

    return stream_df

"""## results of Query 1"""
def send_to_message_query1(lookup_symbols):
    global prevEMA
    #print("send_to_message_query1")
    result_Q1 = ch.ResultQ1()
    list_indicators = list()

    for s in lookup_symbols: # search for symbols required
        if s in prevEMA.index: # if prevEMA tables has information about previous EMAs.
            row = prevEMA.loc[s]
            indicator = ch.Indicator(symbol = row['symbol'], \
                                    ema_38 = row['EMA38'], \
                                    ema_100 =  row['EMA100'] )

        else: # else , send with 0s
            indicator = ch.Indicator(symbol = s, \
                                    ema_38 = 0, \
                                    ema_100 =  0 )
        list_indicators.append(indicator)

    result_Q1.indicators.extend(list_indicators)
    # print(result_Q1.indicators)

    return result_Q1.indicators

def send_to_message_query2():
    global crossover

    crossover = crossover[crossover['BuyOrSell'] != 'Stay']
    tail_crossover = crossover.groupby(['symbol']).tail(3)
    tail_crossover = tail_crossover.reset_index(drop=True)

    # print(tail_crossover)

    result_Q2 = ch.ResultQ2()
    list_crossover = list()

    # serialize
    for i in range(len(tail_crossover)): # search for symbols required
      cross_over_event = ch.CrossoverEvent()
      row = tail_crossover.loc[i ,:]
      cross_over_event.symbol = row["symbol"]

      if row["BuyOrSell"] == "Buy":
        cross_over_event.signal_type = ch.CrossoverEvent.SignalType.Buy
      elif row["BuyOrSell"] == "Sell":
        cross_over_event.signal_type = ch.CrossoverEvent.SignalType.Sell

      if row["Sectype"] == "E" or row["Sectype"] == "0":
        cross_over_event.security_type = ch.SecurityType.Equity
      else:
        cross_over_event.security_type = ch.SecurityType.Index

      cross_over_event.ts.seconds = row['seconds']
      cross_over_event.ts.nanos = row['nanos']

      list_crossover.append(cross_over_event)

    result_Q2.crossover_events.extend(list_crossover)
    # print(result_Q2.crossover_events)

    return result_Q2.crossover_events

"""## Streamed file processing
This funtions saves the stream of stock market data as csv files.
"""
def make_CSV_file(batch_no, events):
    fieldnames = ["id", "symbol", "Sectype", "lasttradeprice", "lastTrade", "seconds", "nanos"]
    event_list_dict = []
    for i in range(len(events)):
            temp_dict = {}
            temp_dict['id'] = i
            temp_dict['symbol'] = events[i].symbol
            temp_dict['Sectype'] = events[i].security_type
            temp_dict['lasttradeprice'] = events[i].last_trade_price
            mills = timedelta(milliseconds= (events[i].last_trade.nanos / 1000000))
            seconds = datetime.fromtimestamp(events[i].last_trade.seconds)
            temp_dict['lastTrade'] = (mills + seconds).strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
            temp_dict['seconds'] = events[i].last_trade.seconds
            temp_dict['nanos'] = events[i].last_trade.nanos
            event_list_dict.append(temp_dict)

    # print(event_list_dict)
    filename = f"./stream/stock_stream-{batch_no}.csv"
    with open(filename, 'w+', encoding='UTF8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(event_list_dict)


def execute_streamingQuery1(df_a):
    global prevEMA, crossover

    for idx, row in tqdm(df_a.iterrows(), total=df_a.shape[0], disable=True):
        symbol = row['symbol']

        if symbol in prevEMA.index: # if existing symbol
            existing_row = prevEMA.loc[symbol]

            new_EMA38 = calcEMA_func(row['lasttradeprice'], existing_row['EMA38'], 38)
            new_EMA100 = calcEMA_func(row['lasttradeprice'], existing_row['EMA100'], 100)

            prevEMA.loc[symbol] = {'symbol': symbol, 'EMA38': new_EMA38, 'EMA100': new_EMA100}

            new_row = {'symbol': symbol, 'lastWindowTime': row['lastWindowTime'], 'EMA38': new_EMA38,
                       'EMA100': new_EMA100, 'Sectype': row['Sectype'], 'BuyOrSell': 'Stay', 'seconds': row['seconds'], 'nanos': row['nanos']}
            crossover = crossover.append(pd.Series(new_row, index=crossover.columns, name=symbol))

        else: # new symbol
            new_EMA38 = calcEMA_func(row['lasttradeprice'], 0.0, 38)
            new_EMA100 = calcEMA_func(row['lasttradeprice'], 0.0, 100)

            new_row = {'symbol': symbol, 'lastWindowTime': row['lastWindowTime'], 'EMA38': new_EMA38,
                       'EMA100': new_EMA100, 'Sectype': row['Sectype'], 'BuyOrSell': 'Stay', 'seconds': row['seconds'], 'nanos': row['nanos']}
            crossover = crossover.append(pd.Series(new_row, index=crossover.columns, name=symbol))

            new_row = {'symbol': symbol, 'EMA38': new_EMA38, 'EMA100': new_EMA100}
            prevEMA = prevEMA.append(pd.Series(new_row, index=prevEMA.columns, name=symbol))


def execute_streamingQuery2():
    global crossover, prevCrossover

    for idx, row in tqdm(crossover.iterrows(), total = crossover.shape[0], disable=True):
        symbol = row['symbol']
        if symbol not in prevCrossover.index:
          diff = row['EMA38'] - row['EMA100']
          new_row = { 'symbol': symbol, 'Diff': diff }
          prevCrossover = prevCrossover.append(pd.Series(new_row, index=prevCrossover.columns, name=symbol))
        # print(prevCrossover)
        existing_row = prevCrossover.loc[symbol]
        BuyOrSell = EMA_Comp( diff, existing_row['Diff'])

        cond = (crossover.symbol == symbol) & (crossover.lastWindowTime == row['lastWindowTime'])
        crossover.loc[cond, ('BuyOrSell')] = BuyOrSell
        prevCrossover.loc[symbol] = { 'symbol': symbol, 'Diff': diff }

def reset_dataframe():
  global prevEMA, crossover, prevCrossover
  crossover = crossover.iloc[0:0]
  prevEMA = prevEMA.iloc[0:0]
  prevCrossover = prevCrossover.iloc[0:0]


# ----------------------------------------------------------------------------------------

# Global Variables
spark = None
logger = None
prevEMA = None
detectCrossover = None
crossover = None
prevCrossover = None

# MAIN Program
def main():
    global spark, logger
    global detectCrossover
    global prevEMA, crossover, prevCrossover

    warnings.filterwarnings("ignore")

    """### initialize spark"""
    # start Spark application and get Spark session, logger and config
    # spark application 시작
    spark = SparkSession.builder.master("local[*]") \
            .config("spark.executor.instances", "6") \
            .config("spark.executor.cores", "15") \
            .config("spark.default.parallelism", "300") \
            .config("spark.executor.memory", "64G") \
            .config('spark.ui.port', '4050') \
            .config("spark.driver.memory", "20g") \
            .config("spark.sql.shuffle.partitions", "300") \
            .config("spark.driver.cores", "12") \
            .config("spark.executor.memory", "20g") \
            .config("spark.memory.offHeap.enabled", "true") \
            .config("spark.memory.offHeap.size", "10g") \
            .config("spark.dynamicAllocation.enabled", "true") \
            .appName("PySpark for DEBS 2022 GC").getOrCreate()


    # spark.conf.set("spark.sql.execution.arrow.pyspark.enabled", "true")
    spark.conf.set("spark.sql.execution.arrow.pyspark.enabled", "true")

    logger = Log4j(spark)
    logger.warn("initialize spark")
    logger.warn(spark.version)

    # init the previous EMA and CrossOver table
    prevEMA = pd.DataFrame({'symbol': pd.Series(dtype='str'),
                            'EMA38': pd.Series(dtype='float'),
                            'EMA100': pd.Series(dtype='float')})
    prevEMA.set_index('symbol')

    crossover = pd.DataFrame({'symbol': pd.Series(dtype='str'),
                              'lastWindowTime': pd.Series(dtype='datetime64[ns]'),
                              'EMA38': pd.Series(dtype='float'),
                              'EMA100': pd.Series(dtype='float'),
                              'Sectype': pd.Series(dtype='str'),
                              'BuyOrSell': pd.Series(dtype='str'),
                              'seconds': pd.Series(dtype='int'),
                              'nanos': pd.Series(dtype='int')})
    prevCrossover = pd.DataFrame({'symbol': pd.Series(dtype='str'),
                                  'Diff': pd.Series(dtype='float')})
    prevCrossover.set_index('symbol')

    """### Define streaming input sources"""
    dir_path = os.path.dirname(os.path.realpath(__file__))
    logger.warn(dir_path)
    stream = read_stream("./stream/")

    """### Transform streaming data and define output sink&mode
    queries 1 and 2
    """
    calcEMA = transform_calcEMA(stream)
    writer1 = calcEMA.writeStream.format("memory") \
        .outputMode("complete") \
        .option("truncate", False) \
        .queryName("EMA") # table

    """### Start the queries"""
    streamingQuery1 = writer1.start()

    """## Challenger system"""
    # setting the option for GRPC
    op = [('grpc.max_send_message_length', 100 * 1024 * 1024),
          ('grpc.max_receive_message_length', 1000 * 1024 * 1024)]

    # Submit results with benchmark
    with grpc.insecure_channel('challenge.msrg.in.tum.de:5023', options=op) as channel:
        stub = api.ChallengerStub(channel)
        # create_benchmark(stub)

        # Step 1 - Create a new Benchmark
        benchmarkconfiguration = ch.BenchmarkConfiguration(token='sdpiwsqcjhlimgruqdhyckrgwjwlawvh',
                                                            benchmark_name="Dept. Computer Engineering, DongA Univ.",
                                                            # This name is used here: https://challenge.msrg.in.tum.de/benchmarks/
                                                            benchmark_type="Evaluation",
                                                            # Test or Evaluation, Evaluation will be available end of January. Test can be used to start implementing
                                                            queries=[ch.Query.Q1, ch.Query.Q2])
        benchmark = stub.createNewBenchmark(benchmarkconfiguration)

        stub.startBenchmark(benchmark)
        event_count = 0
        logger.warn("start benchmark batches")
        while True:
            batch = stub.nextBatch(benchmark)
            event_count = event_count + len(batch.events)
            logger.warn(f"--- BATCH {batch.seq_id}: ID {benchmark.id} ")

            # batch.symbol로부터 받은 주식ID추출
            lookup_symbols = list(batch.lookup_symbols)
            # if batch.seq_id == 1977:
            #     logger.warn(f"list for lookup: {lookup_symbols}")
            # print(lookup_symbols)

            # 1. every batch data is stored as csv files in the specific folder
            make_CSV_file(batch.seq_id, batch.events)

            # 2. update the previous EMA table
            lastTrades = spark.sql("select * from EMA").filter(col('symbol').isin(lookup_symbols)).toPandas()
            execute_streamingQuery1(lastTrades)

            # 3. send the result of query 1 to the challenger system
            resultQ1 = ch.ResultQ1(
                benchmark_id=benchmark.id,  # The id of the benchmark
                batch_seq_id=batch.seq_id,  # The sequence id of the batch
                indicators=send_to_message_query1(lookup_symbols))  # Query1을 계산하여 challenge 서버에 보낼 message 생성
            stub.resultQ1(resultQ1)  # send the result of query 1 back

            # 4. update the previous crossover table
            execute_streamingQuery2()

            # 5. send the result of query 2 to the challenger system
            resultQ2 = ch.ResultQ2(
                benchmark_id=benchmark.id,  # The id of the benchmark
                batch_seq_id=batch.seq_id,  # The sequence id of the batch
                crossover_events=send_to_message_query2())
                    # create the result of query 2 by calculating the last 3 crossover data

            stub.resultQ2(resultQ2)  # submit the results of Q2

            reset_dataframe()

            if batch.last:
                print(f"received last batch, total batches: {event_count}")
                stub.endBenchmark(benchmark)
                break

        print(f"Completed.")
        spark.stop()

# entry point for PySpark ETL application
if __name__ == '__main__':
    main()
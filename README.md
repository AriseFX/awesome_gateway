Awesome Gateway
---
Awesome Gateway是一个基于Netty实现的高性能api网关。已经在公司生产环境跑了两年，除了一些业务相关的过滤器之外，其他全部开源。

--------------------------------------------------------------------------------
# 功能特点

* 支持 Http1.x 转发
* 支持 Nacos 配置/注册中心（可自行拓展）
* 支持动态路由
* 用户可以轻松定制化开发
* 磁盘队列（用于保证日志性能）
* 高性能
--------------------------------------------------------------------------------
# 快速开始

### 1.构建
```
1. git clone https://github.com/AriseFX/awesome_gateway.git

2. mvn package
```
### 2.配置

默认使用Redis存储后台数据，如需其他数据源请自行拓展。

配置模版:
```yml
port: 7778 #网关访问端口
address: 0.0.0.0
respTimeout: 30000 #后端响应超时时间
pool:
  timeout: 30000 #连接超时时间
  maxConnections: 4000 #最大连接数
  maxPendingAcquires: 0x7fffffff #最大pending数
registry:
  naming: nacos
  serverAddr: xxx.xx.xx.xx:xxxx
  namespace: xxx
  serviceName: AsGateway
  networkInterface: eth0
logging:
  excludePath: [ "/selectLogByCondition" ]
  #"Content-Type":["application/json","text/html"]代表存储这两种消息体
  reqHeader: { "Content-Type": [ "application/xml","application/json","application/json;charset=utf-8","text/xml" ,"text/xml; charset=utf-8" ] }
  respHeader: { "Content-Type": [ "text/html","application/json","application/json;charset=utf-8","text/xml","text/xml; charset=utf-8" ] }
redis:
  #具体参考Redis URI规则
  uri: redis://password@ip:port/db?timeout=10s
#rabbitmq:
#  uri: amqp://admin:admin@xxxxx:5672/dev
endpoint:
  port: 9876 #网关暴露的endpoint
  address: 127.0.0.1
```
### 2.添加路由

网关默认暴露了4个端点
```
1. /route/get 获取所有路由
2. /route/put 添加/修改路由
3. /route/refresh 刷新路由(清空缓存并从存储中加载缓存)
4. /route/clear 清除路由缓存
5. /metrics 网关运维指标    
```
```
curl --location --request POST 'localhost:9876/route/put' \
--header 'Content-Type: application/json' \
--data-raw '{
    "routes": [
        {
            "id": "1",
            "service": "lb://TOMCAT",
            "tag": "TOMCAT",
            "gatewayPath": "/test",
            "servicePath": "/test123",
            "metadata": {
                "key": "value"
            }
        }
    ]
}'
```
启动了一个应用「TOMCAT」并注册到了Nacos上，那么可以使用`lb://`格式来配置路由，如curl中的service，
也可以把lb换成http或https。配置中gatewayPath为网关路由，servicePath为对于的后端路由。
路由添加完成就可以使用`/route/refresh`来刷新路由了。

### 3.性能测试

直接使用上述的配置进行性能测试，后面把本网关简称「AWE」:

#### 测试环境: openjdk8。 组件: wrk,tomcat,nginx
#### 机器:
```
linux1: 8核,192.168.0.182,压测工具
linux2: 8核,192.168.0.109,代理
linux3: 8核,192.168.0.3,应用
```
---
压测命令:
```
//8个线程,2000个连接,持续30秒,超时时间150秒
wrk -t8 -c2000 -d 30s -T150s --latency http://192.168.0.3:41607/test?id=1 
```


### 开始压测:

#### 一.直连(不经过任何代理): wrk -> tomcat
```
第一轮:
Running 30s test @ http://192.168.0.3:41041/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    58.23ms    6.85ms 139.87ms   83.79%
    Req/Sec     4.31k   383.32     5.61k    78.14%
  Latency Distribution
     50%   57.25ms
     75%   60.45ms
     90%   64.56ms
     99%   82.78ms
  1025845 requests in 30.02s, 2.82GB read
Requests/sec:  34176.77
Transfer/sec:     96.15MB

第二轮:
Running 30s test @ http://192.168.0.3:41041/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    58.30ms    7.79ms 284.87ms   84.84%
    Req/Sec     4.30k   340.08     5.18k    75.50%
  Latency Distribution
     50%   57.41ms
     75%   60.47ms
     90%   65.59ms
     99%   82.84ms
  1024768 requests in 30.07s, 2.82GB read
Requests/sec:  34077.51
Transfer/sec:     95.87MB
```
#### 该tomcat服务器的极限吞吐量是: 「 34127.14 op/s 」

----
#### 二.经过网关

```
如何衡量一个网关的性能? 
1.代理所产生的损耗
2.网关的极限ops有多大
```

### 1. Awe代理:

```
wrk -> AWE -> tomcat (开启所有插件,保存完整日志)
```
```
第一轮:
Running 30s test @ http://192.168.0.109:7778/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    59.53ms    9.30ms 272.55ms   89.48%
    Req/Sec     4.22k   438.28     5.40k    81.02%
  Latency Distribution
     50%   58.12ms
     75%   61.44ms
     90%   66.14ms
     99%   90.88ms
  1004701 requests in 30.02s, 2.76GB read
Requests/sec:  33467.78
Transfer/sec:     94.18MB

第二轮:
Running 30s test @ http://192.168.0.109:7778/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    71.88ms   89.61ms 962.67ms   97.40%
    Req/Sec     4.27k   462.78     5.09k    81.53%
  Latency Distribution
     50%   57.37ms
     75%   61.17ms
     90%   71.52ms
     99%   91.12ms
  991759 requests in 30.07s, 2.73GB read
Requests/sec:  32985.51
Transfer/sec:     92.82MB
```
### 得出结果:
```
平均值: 33226.645 op/s
损耗: 2.63%
```


### 2. Nginx代理:
```
wrk -> nginx -> tomcat
```
```
第一轮:
Running 30s test @ http://192.168.0.109:7777/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    58.90ms    7.43ms 184.32ms   84.45%
    Req/Sec     4.26k   339.32     5.03k    72.53%
  Latency Distribution
     50%   57.90ms
     75%   61.29ms
     90%   66.04ms
     99%   79.60ms
  1014773 requests in 30.02s, 2.83GB read
Requests/sec:  33797.69
Transfer/sec:     96.58MB

第二轮:
Running 30s test @ http://192.168.0.109:7777/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    60.04ms    7.37ms 125.31ms   83.82%
    Req/Sec     4.18k   388.99     5.19k    78.22%
  Latency Distribution
     50%   58.88ms
     75%   62.54ms
     90%   66.89ms
     99%   85.64ms
  995008 requests in 30.05s, 2.78GB read
Requests/sec:  33114.21
Transfer/sec:     94.63MB
```
### 得出结果
```
平均值:  33455.95 op/s 
损耗: 1.96%
```
### 3. SpringCloud Gateway代理:
```
wrk -> SpringCloud Gateway -> tomcat
```
```
第一轮:
Running 30s test @ http://192.168.0.109:8084/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    78.44ms   28.60ms 567.53ms   87.17%
    Req/Sec     3.24k   356.36     3.93k    76.05%
  Latency Distribution
     50%   75.15ms
     75%   87.79ms
     90%  102.36ms
     99%  144.76ms
  771332 requests in 30.08s, 2.12GB read
Requests/sec:  25644.53
Transfer/sec:     72.20MB

第二轮:
Running 30s test @ http://192.168.0.109:8084/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    77.94ms   28.85ms 913.87ms   87.73%
    Req/Sec     3.26k   306.57     4.11k    72.70%
  Latency Distribution
     50%   74.94ms
     75%   87.66ms
     90%  101.97ms
     99%  138.76ms
  775034 requests in 30.05s, 2.13GB read
Requests/sec:  25793.21
Transfer/sec:     72.62MB
```
### 得出结果:
```
平均值: 25718.87 op/s 
损耗: 24.64%
```
### 小结

| 压测路径                  | 吞吐量  | 损耗
|-----------------------| ----  | ----
| 直连                    | 34127.14 | -
| AWE代理                 | 33226.65 | 2.63%
| Nginx代理               | 33455.95 | 1.96%
| SpringCloud Gateway代理 | 25718.87 | 24.64%

### 下面测试极限tps (把cpu跑满)

为了能把网关服务器跑满，后端用tomcat肯定是不够的，所以就直接启了一个Nginx作为后端服务。

#### wrk -> AWE -> nginx(返回固定json)

```
Running 30s test @ http://192.168.0.109:7778/test1?id=1
  16 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    19.78ms    3.18ms  59.16ms   70.93%
    Req/Sec     6.35k   555.97    24.75k    99.00%
  Latency Distribution
     50%   19.18ms
     75%   22.01ms
     90%   24.11ms
     99%   27.39ms
  3041131 requests in 30.10s, 8.39GB read
Requests/sec: 101039.46
Transfer/sec:    285.32MB
```

#### wrk -> nginx -> nginx(返回固定json)

```
Running 30s test @ http://192.168.0.109:7777/test1?id=1
  16 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    18.64ms   29.90ms   1.05s    99.66%
    Req/Sec     7.24k     0.88k   55.35k    98.81%
  Latency Distribution
     50%   16.57ms
     75%   19.29ms
     90%   22.23ms
     99%   33.83ms
  3461967 requests in 30.10s, 9.55GB read
Requests/sec: 115018.13
Transfer/sec:    324.79MB
```

#### wrk -> SpringCloud Gateway -> nginx(返回固定json)

```
Running 30s test @ http://192.168.0.109:8084/test?id=1
  8 threads and 2000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   123.39ms  281.50ms   3.47s    96.38%
    Req/Sec     3.34k     1.01k    4.45k    84.47%
  Latency Distribution
     50%   68.97ms
     75%   85.33ms
     90%  126.43ms
     99%    1.88s
  786080 requests in 30.06s, 2.15GB read
Requests/sec:  26151.00
Transfer/sec:     73.25MB
```
### 小结
|     | 直连  | AWE代理     | Nginx代理 | SpringCloud Gateway代理
|  ----  | ----  |-----------| ---- | ----
| 极限吞吐量 | 146292.88 | 101039.46 | 115018.13 | 26151.00

### 总结
```
从损耗和极限TPS来看，AWE与Nginx性能接近，远胜于Springcloud Gateway
```


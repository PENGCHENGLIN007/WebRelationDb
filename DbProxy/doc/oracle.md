# Oracle协议分析
oracle使用TNS协议
## TNS协议分析
TNS协议头有如下字段：
1.Packet Length 2字节长度（大端），表示整个请求的长度，包括TNS头和数据
2.Packet Type 1字节，表示请求类型，0x01表示Connect
3.03 5e表示后面有SQL语句


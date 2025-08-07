# java试题生成器
## 1. 使用通义千问3模型生成试题，包括选择题、判断题、问答题
## 2.可选模型14b、32b、235b,其中235b效果最好，也最快
## 3.生成时间大概45s以上
## 4.需要配合nginx使用，application-config.yml中的 
```
  # 即是nginx的访问文件路径
  base-url: http://localhost/java/
  #nginx 静态文件存放的目录
  file-path: /mnt/f/files/java
```
## 5.原理是，根据用户选择生成提示词，调用阿里云大模型接口生成html代码，用java写入nginx静态文件目录，用java中的index.html里面的iframe加载生成的html文件路径
## 6.网址： https://java.2u1.cn/

<img width="1908" height="951" alt="image" src="https://github.com/user-attachments/assets/30f5d907-4387-452c-9a0f-70d695622f7b" />

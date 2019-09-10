import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

import java.io.*;

/**
 * 用于导入redis数据
 * User: zhaodp
 * Date: 2016-7-15
 */
public class RedisLoader {
    @Parameter(names = {"--host", "-h"}, description = "redis server host")
    String host;
    @Parameter(names = {"--port", "-p"}, description = "redis server host")
    int port = 6379;
    @Parameter(names = {"--password", "-a"}, description = "redis auth")
    String password;
    /*@Parameter(names = {"--index", "-i"}, description = "redis db index")
    int index;*/
    /*@Parameter(names = {"--dataPath", "-d"}, description = "where is the data to be loaded ")
    String datePath;*/

    @Parameter(names={"--folder","-f"},description = "folderName")
    String folder;

    public static void main(String[] args) {
        RedisLoader redisDumper = new RedisLoader();
        /*String[] argvs = {"-h","127.0.0.1","-p","6379","-f","E:/redisBack/20190909173000"};*/
        new JCommander(redisDumper, args);
        redisDumper.doLoad();
    }

    private void doLoad() {
        Jedis jedis = new Jedis(host,port);
        if(password != null)
            jedis.auth(password);
        for (int index = 0; index < 16; index++) {
            jedis.select(index);

            BufferedReader br = null;
            try {
                File dataFile = new File(folder+File.separator+"db["+index+"].txt");
                FileReader fileReader = new FileReader(dataFile);
                br = new BufferedReader(fileReader);

                String line;
                while((line = br.readLine()) != null){
                    line = line.trim();
                    if("".equals(line)){
                        continue;
                    }
                    Gson gson = new Gson();
                    BasicKeyValue basicKeyValue = gson.fromJson(line,BasicKeyValue.class);
                    if("string".equals(basicKeyValue.type)){
                        StringKeyValue.loadJsonIntoRedis(jedis,line);
                    }else if("zset".equals(basicKeyValue.type)){
                        ZSetKeyValue.loadJsonIntoRedis(jedis,line);
                    }else if("set".equals(basicKeyValue.type)){
                        SetKeyValue.loadJsonIntoRedis(jedis,line);
                    }else if("list".equals(basicKeyValue.type)){
                        ListKeyValue.loadJsonIntoRedis(jedis,line);
                    }else if("hash".equals(basicKeyValue.type)){
                        HashKeyValue.loadJsonIntoRedis(jedis,line);
                    }
                }
                System.out.println("db["+index+"] is already back up");

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
                if(br != null){
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("all dbs are back up!");
    }
}
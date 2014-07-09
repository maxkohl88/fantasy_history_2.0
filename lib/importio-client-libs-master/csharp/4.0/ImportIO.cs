using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net;
using System.IO;
using System.Web;
using System.Net;

// Download the Newtonsoft JSON library here http://james.newtonking.com/projects/json-net.aspx
using Newtonsoft.Json;
using System.Threading;
using System.Collections.Concurrent;

namespace MinimalCometLibrary
{
    delegate void QueryHandler(Query query, Dictionary<String, Object> data);

    class Query
    {
        int jobsCompleted = 0;
        int jobsStarted = 0;
        int jobsSpawned = 0;
        private bool finished = false;

        public bool isFinished { get { return finished; } set { finished = value; } }
        public QueryHandler queryCallback;

        Dictionary<String, Object> queryInput;

        public Query(Dictionary<String, Object> queryInput, QueryHandler queryCallback)
        {
            this.queryInput = queryInput;
            this.queryCallback = queryCallback;
        }

        public void OnMessage(Dictionary<String, Object> data)
        {
            String messageType = (String)data["type"];

            Console.WriteLine((String)data["type"]);

            switch (messageType)
            {
                case "SPAWN":
                    jobsSpawned++;
                    break;
                case "INIT":
                case "START":
                    jobsStarted++;
                    break;
                case "STOP":
                    jobsCompleted++;
                    break;
            }

            finished = jobsStarted == jobsCompleted && jobsSpawned + 1 == jobsStarted && jobsStarted > 0;

            if (messageType.Equals("ERROR") || messageType.Equals("UNAUTH") || messageType.Equals("CANCEL"))
            {
                finished = true;
            }

            queryCallback(this, data);
        }

    }

    class ImportIO
    {
        private String host { get; set; }
        private int port { get; set; }

        private Guid userGuid;
        private String apiKey;

        private static String messagingChannel = "/messaging";
        private String url;

        private int msgId = 0;
        private String clientId;

        private Boolean isConnected;

        CookieContainer cookieContainer = new CookieContainer();

        Dictionary<Guid, Query> queries = new Dictionary<Guid, Query>();

        private BlockingCollection<Dictionary<String, Object>> messageQueue = new BlockingCollection<Dictionary<string,object>>();

        public ImportIO(String host = "http://query.import.io", Guid userGuid = default(Guid), String apiKey = null)
        {
            this.userGuid = userGuid;
            this.apiKey = apiKey;

            this.url = host + "/query/comet/";
            clientId = null;
        }

        public void Login(String username, String password, String host = "http://api.import.io")
        {
            Console.WriteLine("Logging in");
            String loginParams = "username=" + HttpUtility.UrlEncode(username) + "&password=" + HttpUtility.UrlEncode(password);
            String searchUrl = host + "/auth/login";
            HttpWebRequest loginRequest = (HttpWebRequest)WebRequest.Create(searchUrl);

            loginRequest.Method = "POST";
            loginRequest.ContentType = "application/x-www-form-urlencoded";
            loginRequest.ContentLength = loginParams.Length;

            loginRequest.CookieContainer = cookieContainer;

            using (Stream dataStream = loginRequest.GetRequestStream())
            {
                dataStream.Write(System.Text.UTF8Encoding.UTF8.GetBytes(loginParams), 0, loginParams.Length);

                HttpWebResponse loginResponse = (HttpWebResponse)loginRequest.GetResponse();


                if (loginResponse.StatusCode != HttpStatusCode.OK)
                {
                    throw new Exception("Could not log in, code:" + loginResponse.StatusCode);
                }
                else
                {
                    foreach (Cookie cookie in loginResponse.Cookies)
                    {
                        if (cookie.Name.Equals("AUTH"))
                        {
                            // Login was successful
                            Console.WriteLine("Login Successful");
                        }
                    }

                }
            }
        }

        public List<Dictionary<String, Object>> Request(String channel, Dictionary<String, Object> data = null, String path = "", Boolean doThrow = true)
        {
            Dictionary<String, Object> dataPacket = new Dictionary<String, Object>();
            dataPacket.Add("channel", channel);
            dataPacket.Add("connectionType", "long-polling");
            dataPacket.Add("id", (msgId++).ToString());

            if (this.clientId != null)
                dataPacket.Add("clientId", this.clientId);

            if (data != null)
            {
                foreach (KeyValuePair<String, Object> entry in data)
                {
                    dataPacket.Add(entry.Key, entry.Value);
                }
            }

            String url = this.url + path;

            if (apiKey != null)
            {
                url += "?_user=" + HttpUtility.UrlEncode(userGuid.ToString()) + "&_apikey=" + HttpUtility.UrlEncode(apiKey);
            }

            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.AutomaticDecompression = DecompressionMethods.GZip;
            request.Method = "POST";
            request.ContentType = "application/json;charset=UTF-8";
            request.Headers.Add(HttpRequestHeader.AcceptEncoding, "gzip");
            String dataJson = JsonConvert.SerializeObject(new List<Object>() { dataPacket });

            request.ContentLength = dataJson.Length;

            request.CookieContainer = cookieContainer;

            using (Stream dataStream = request.GetRequestStream())
            {
                dataStream.Write(System.Text.UTF8Encoding.UTF8.GetBytes(dataJson), 0, dataJson.Length);
                try
                {
                    HttpWebResponse response = (HttpWebResponse)request.GetResponse();

                    using (StreamReader responseStream = new StreamReader(response.GetResponseStream()))
                    {
                        String responseJson = responseStream.ReadToEnd();
                        List<Dictionary<String, Object>> responseList = JsonConvert.DeserializeObject<List<Dictionary<String, Object>>>(responseJson);
                        foreach (Dictionary<String, Object> responseDict in responseList)
                        {
                            if (responseDict.ContainsKey("successful") && (bool)responseDict["successful"] != true)
                            {
                                if (doThrow)
                                    throw new Exception("Unsucessful request");
                            }

                            if (!responseDict["channel"].Equals(messagingChannel)) continue;

                            if (responseDict.ContainsKey("data"))
                            {
                                messageQueue.Add(((Newtonsoft.Json.Linq.JObject)responseDict["data"]).ToObject<Dictionary<String, Object>>());
                            }

                        }

                        return responseList;
                    }
                }
                catch (Exception e)
                {
                    Console.WriteLine("Error occurred {0}", e.Message);
                    return new List<Dictionary<String, Object>>();
                }
                
            }
        }

        public void Handshake()
        {
            Dictionary<String, Object> handshakeData = new Dictionary<String, Object>();
            handshakeData.Add("version", "1.0");
            handshakeData.Add("minimumVersion", "0.9");
            handshakeData.Add("supportedConnectionTypes", new List<String> { "long-polling" });
            handshakeData.Add("advice", new Dictionary<String, int>() { { "timeout", 60000 }, { "interval", 0 } });
            List<Dictionary<String, Object>> responseList = Request("/meta/handshake", handshakeData, "handshake");
            clientId = (String)responseList[0]["clientId"];
        }

        public void Connect()
        {
            if(isConnected) {
                return ;
            }
            
            Handshake();

            Dictionary<String, Object> subscribeData = new Dictionary<string, object>();
            subscribeData.Add("subscription", messagingChannel);
            Request("/meta/subscribe", subscribeData);

            isConnected = true;

            new Thread(new ThreadStart(Poll)).Start();

            new Thread(new ThreadStart(PollQueue)).Start();
        }

        public void Disconnect()
        {
            Request("/meta/disconnect", null, "", true);
            isConnected = false;
        }

        private void Poll()
        {
            while (isConnected)
            {
                Request("/meta/connect", null, "connect", false);
            }
        }

        private void PollQueue()
        {
            while (isConnected)
            {
                ProcessMessage(messageQueue.Take());
            }
        }

        private void ProcessMessage(Dictionary<String, Object> data)
        {
            Guid requestId = Guid.Parse((String)data["requestId"]);
            Query query = queries[requestId];

            query.OnMessage(data);
            if (query.isFinished)
            {
                queries.Remove(requestId);
            }
        }

        public void DoQuery(Dictionary<String, Object> query, QueryHandler queryHandler)
        {
            Guid requestId = Guid.NewGuid();
            queries.Add(requestId, new Query(query, queryHandler));
            query.Add("requestId", requestId);
            Request("/service/query", new Dictionary<String, Object>() { { "data", query } });
        }


    }
}

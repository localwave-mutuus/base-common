"""Loopback-only TLS HTTP/2 redirect and REFUSED_STREAM retry fixture (h2 4.3.0)."""
import socket, ssl, threading, sys, json
import h2.connection, h2.config, h2.events
lock = threading.Lock()
wires = []
retry = 0
listener = socket.socket()
listener.bind(("127.0.0.1", 0))
listener.listen()
port = listener.getsockname()[1]
tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
tls.load_cert_chain("target/harness/h2-cert.pem", "target/harness/h2-key.pem")
tls.set_alpn_protocols(["h2"])
def serve(raw):
    global retry
    try:
        with tls.wrap_socket(raw, server_side=True) as conn:
            assert conn.selected_alpn_protocol() == "h2"
            proto = h2.connection.H2Connection(config=h2.config.H2Configuration(client_side=False, header_encoding="utf-8"))
            proto.initiate_connection()
            conn.sendall(proto.data_to_send())
            while data := conn.recv(65536):
                for event in proto.receive_data(data):
                    if isinstance(event, h2.events.RequestReceived):
                        headers = dict(event.headers)
                        path = headers[":path"]
                        approved = headers[":authority"].startswith("localhost:")
                        with lock:
                            wires.append(dict(approved=approved, trace="traceparent" in headers,
                                              state="tracestate" in headers, baggage="baggage" in headers))
                            refuse = path == "/retry" and retry == 0
                            if path == "/retry": retry += 1
                            if len(wires) == int(sys.argv[1] if len(sys.argv)>1 else "6"):
                                with open("target/harness/h2-wire.json", "w") as out: json.dump(wires, out)
                        if refuse:
                            proto.reset_stream(event.stream_id, error_code=7)
                        elif path in ("/forward", "/reverse"):
                            host = "127.0.0.1" if path == "/forward" else "localhost"
                            proto.send_headers(event.stream_id, [(":status", "302"), ("location", f"https://{host}:{port}/end")], end_stream=True)
                        else:
                            proto.send_headers(event.stream_id, [(":status", "204")], end_stream=True)
                conn.sendall(proto.data_to_send())
    except (OSError, ssl.SSLError):
        pass
print(port, flush=True)
while True:
    raw, _ = listener.accept()
    threading.Thread(target=serve, args=(raw,), daemon=True).start()
